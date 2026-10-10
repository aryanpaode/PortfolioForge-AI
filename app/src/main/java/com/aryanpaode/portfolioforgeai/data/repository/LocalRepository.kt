package com.aryanpaode.portfolioforgeai.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.aryanpaode.portfolioforgeai.data.db.AppDatabase
import com.aryanpaode.portfolioforgeai.data.db.WorkspaceDatabases
import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.service.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class ImportOutcome(val document: DocumentEntity, val duplicate: Boolean = false)
data class SaveReviewOutcome(val added: Int, val updated: Int, val duplicateEntries: Int, val newSkills: Int)

class LocalRepository(
    private val context: Context,
    private val databases: WorkspaceDatabases,
    private val auth: StateFlow<AuthState>,
    private val ocrService: OcrService,
    private val classifier: DocumentClassifier,
    private val skillIntelligence: SkillIntelligence
) {
    val workspaceId: String get() = auth.value.workspaceId
    private fun database(key: String = workspaceId) = databases.get(key)
    private fun checkWorkspace(key: String) = check(key == workspaceId) { "The account changed; retry in the current workspace." }

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<Snapshot> = auth.map { it.workspaceId }.distinctUntilChanged().flatMapLatest { key ->
        val db = database(key)
        combine(db.profileDao().observe(), db.documentDao().observeAll(), db.careerItemDao().observeAll(), db.skillDao().observeAll()) { p, d, i, s ->
            Snapshot(p, d, i, s, key)
        }.onStart { emit(Snapshot.empty(key)) }
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        val p = profile.copy(githubUrl = validLink(profile.githubUrl), linkedInUrl = validLink(profile.linkedInUrl), portfolioUrl = validLink(profile.portfolioUrl))
        database().profileDao().upsert(p)
    }
    suspend fun addCareerItem(item: CareerItemEntity) {
        require(item.title.isNotBlank()) { "Enter the title/topic first." }
        require(item.type in CareerType.values().map { it.label }) { "Choose a valid section." }
        if (item.type == "Publication" && item.date.isNotBlank()) require(Regex("\\d{4}").matches(item.date.trim())) { "Publication year must have four digits, for example 2026." }
        database().careerItemDao().upsert(item.copy(title = item.title.trim(), subtitle = item.subtitle.trim(), date = item.date.trim(), description = item.description.trim(), link = validLink(item.link)))
    }
    suspend fun deleteCareerItem(item: CareerItemEntity) = database().careerItemDao().delete(item)
    suspend fun deleteSkill(skill: SkillEntity) = database().skillDao().delete(skill)
    suspend fun addSkill(name: String) {
        val clean = skillIntelligence.normalizeName(name)
        require(clean.isNotBlank()) { "Enter a skill name." }
        val db = database()
        db.withTransaction { insertSkills(db, listOf(SkillEntity(UUID.randomUUID().toString(), clean, skillIntelligence.categorize(clean)))) }
    }

    private suspend fun insertSkills(db: AppDatabase, incoming: List<SkillEntity>): Int {
        val known = db.skillDao().getAll().map { skillIntelligence.normalizeName(it.name).lowercase() }.toMutableSet()
        val additions = incoming.mapNotNull { s ->
            val clean = skillIntelligence.normalizeName(s.name)
            if (clean.isBlank() || !known.add(clean.lowercase())) null else s.copy(name = clean, category = skillIntelligence.categorize(clean))
        }
        if (additions.isNotEmpty()) db.skillDao().insertAll(additions)
        return additions.size
    }

    suspend fun importDocument(uri: Uri, hint: String = "Auto detect"): ImportOutcome {
        val key = workspaceId
        return withContext(Dispatchers.IO) {
            val db = database(key)
            val resolver = context.contentResolver
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
                ?: "document_${System.currentTimeMillis()}"
            val mime = resolver.getType(uri) ?: when (name.substringAfterLast('.').lowercase()) {
                "pdf" -> "application/pdf"; "png" -> "image/png"; else -> "image/jpeg"
            }
            require(mime.startsWith("image/") || mime == "application/pdf" || name.endsWith(".pdf", true)) { "Select a PDF, JPG or PNG document." }
            val id = UUID.randomUUID().toString()
            val ext = when { mime == "application/pdf" || name.endsWith(".pdf", true) -> "pdf"; mime == "image/png" -> "png"; mime == "image/webp" -> "webp"; else -> "jpg" }
            val folder = File(context.filesDir, "documents/${WorkspaceDatabases.folder(key)}").apply { mkdirs() }
            val target = File(folder, "$id.$ext")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                resolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "The selected document could not be opened." }
                    target.outputStream().use { out ->
                        val buffer = ByteArray(8192); var total = 0L
                        while (true) {
                            val n = input.read(buffer); if (n < 0) break
                            total += n
                            require(total <= MAX_FILE_BYTES) { "This file is larger than 50 MB. Split/compress it before uploading." }
                            digest.update(buffer, 0, n); out.write(buffer, 0, n)
                        }
                        require(total > 0) { "The selected file is empty." }
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                var same = db.documentDao().findByHash(hash)
                // Old version-1 files did not store checksums. Backfill only in this workspace.
                if (same == null) {
                    for (old in db.documentDao().getAll().filter { it.contentHash.isBlank() && it.localPath.isNotBlank() }) {
                        val file = File(old.localPath)
                        if (file.isFile) {
                            val oldHash = fileHash(file)
                            db.documentDao().upsert(old.copy(contentHash = oldHash))
                            if (oldHash == hash) { same = old.copy(contentHash = oldHash); break }
                        }
                    }
                }
                checkWorkspace(key)
                if (same != null) {
                    val existing = same
                    if (existing.localPath.isBlank() || !File(existing.localPath).isFile) {
                        val attached = existing.copy(localPath = target.absolutePath, ocrWarning = "")
                        db.documentDao().upsert(attached)
                        return@withContext ImportOutcome(attached, true)
                    }
                    target.delete()
                    return@withContext ImportOutcome(existing, true)
                }
                val read = try { ocrService.extract(target, mime) } catch (e: CancellationException) { throw e }
                catch (e: Exception) { OcrResult("", "Text extraction failed: ${e.localizedMessage ?: "unreadable file"}. You can still enter the details manually.") }
                checkWorkspace(key)
                val entity = DocumentEntity(id, name, target.absolutePath, mime, classifier.classify(read.text, name, hint),
                    extractedText = read.text, verifiedText = read.text, contentHash = hash, ocrWarning = read.warning)
                db.documentDao().upsert(entity)
                ImportOutcome(entity)
            } catch (e: Exception) { target.delete(); throw e }
        }
    }

    suspend fun rereadDocument(id: String, forceOcr: Boolean = true) {
        val key = workspaceId; val db = database(key)
        val doc = db.documentDao().getById(id) ?: error("Document not found.")
        require(doc.localPath.isNotBlank()) { "Only cloud text is available. Re-upload the original PDF/image to read it again." }
        val read = ocrService.extract(File(doc.localPath), doc.mimeType, forceOcr)
        checkWorkspace(key)
        // Only the draft text changes. Previously approved career entries are never erased here.
        db.documentDao().upsert(doc.copy(extractedText = read.text, verifiedText = read.text, verified = false, ocrWarning = read.warning))
    }

    suspend fun saveReviewedDocument(review: DocumentReview, selectedItems: List<CareerItemEntity>, selectedSkills: List<SkillEntity>, profile: ProfileEntity? = null): SaveReviewOutcome {
        val key = workspaceId; val db = database(key)
        selectedItems.forEach { item ->
            require(item.type in CareerType.values().map { it.label }) { "Choose a valid destination section." }
            require(item.title.isNotBlank()) { "An included entry is missing its title. Fill it in or uncheck that entry." }
            validLink(item.link)
            if (item.type == "Publication" && item.date.isNotBlank()) require(Regex("\\d{4}").matches(item.date.trim())) { "Publication year must be four digits." }
        }
        return db.withTransaction {
            checkWorkspace(key)
            val doc = db.documentDao().getById(review.documentId) ?: error("Document not found.")
            var added = 0; var updated = 0; var duplicates = 0
            val existing = db.careerItemDao().getAll().toMutableList()
            for (draft in selectedItems) {
                val item = draft.copy(title = draft.title.trim(), subtitle = draft.subtitle.trim(), date = draft.date.trim(), link = validLink(draft.link), sourceDocumentId = doc.id)
                val sameId = existing.firstOrNull { it.id == item.id && it.sourceDocumentId == doc.id }
                val sameFacts = existing.firstOrNull { EntryIdentity.same(it, item) }
                when {
                    sameId != null -> { db.careerItemDao().upsert(item); existing.remove(sameId); existing += item; updated++ }
                    sameFacts != null -> { duplicates++ } // Keep an already approved/manual record rather than overwrite it.
                    else -> { db.careerItemDao().upsert(item); existing += item; added++ }
                }
            }
            val skillCount = insertSkills(db, selectedSkills.map { it.copy(source = if (review.skillsAreTopics) "Conference topic confirmed by user: ${doc.fileName}" else "Verified: ${doc.fileName}") })
            if (profile != null) mergeProfile(db, profile)
            db.documentDao().upsert(doc.copy(category = review.category, verifiedText = review.text.trim(), verified = true,
                importedAt = if (selectedItems.isNotEmpty()) System.currentTimeMillis() else doc.importedAt))
            SaveReviewOutcome(added, updated, duplicates, skillCount)
        }
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        val db = database()
        val doc = db.documentDao().getById(id) ?: return@withContext
        if (doc.localPath.isNotBlank()) File(doc.localPath).delete()
        db.documentDao().delete(doc)
        // Career entries and approved skills are intentionally retained. Delete them in their own sections.
    }

    suspend fun snapshot(key: String = workspaceId): Snapshot {
        val db = database(key)
        return db.withTransaction { Snapshot(db.profileDao().get(), db.documentDao().getAll(), db.careerItemDao().getAll(), db.skillDao().getAll(), key) }
    }

    private suspend fun mergeProfile(db: AppDatabase, fresh: ProfileEntity) {
        val old = db.profileDao().get() ?: ProfileEntity()
        fun keep(a: String, b: String) = a.ifBlank { b }
        db.profileDao().upsert(old.copy(name = keep(old.name, fresh.name), email = keep(old.email, fresh.email), phone = keep(old.phone, fresh.phone),
            branch = keep(old.branch, fresh.branch), semester = keep(old.semester, fresh.semester), cgpa = keep(old.cgpa, fresh.cgpa),
            githubUrl = keep(old.githubUrl, fresh.githubUrl), linkedInUrl = keep(old.linkedInUrl, fresh.linkedInUrl), portfolioUrl = keep(old.portfolioUrl, fresh.portfolioUrl),
            targetRole = keep(old.targetRole, fresh.targetRole), summary = keep(old.summary, fresh.summary)))
    }

    /** Restore is a non-destructive merge. Files restored from cloud are metadata-only. */
    suspend fun restoreStructuredData(cloud: CloudStructuredData) {
        checkWorkspace(cloud.ownerUid)
        val db = database(cloud.ownerUid)
        db.withTransaction {
            checkWorkspace(cloud.ownerUid)
            cloud.profile?.let { mergeProfile(db, it) }
            val existing = db.careerItemDao().getAll().toMutableList()
            cloud.items.filter { it.id.isNotBlank() && it.title.isNotBlank() }.forEach { item ->
                if (existing.none { it.id == item.id || EntryIdentity.same(it, item) }) { db.careerItemDao().upsert(item); existing += item }
            }
            insertSkills(db, cloud.skills.filter { it.id.isNotBlank() })
            cloud.documents.filter { it.id.isNotBlank() }.forEach { doc ->
                if (db.documentDao().getById(doc.id) == null) db.documentDao().upsert(doc.copy(localPath = "", extractedText = ""))
            }
        }
    }

    /** Explicit one-time adoption. No guest data is ever silently attached to a Firebase account. */
    suspend fun moveLocalWorkspaceToAccount() = withContext(Dispatchers.IO) {
        val key = workspaceId
        require(key != WorkspaceDatabases.LOCAL) { "Sign in before moving local data." }
        val targetDb = database(key); val guestDb = database(WorkspaceDatabases.LOCAL)
        val target = snapshot(key)
        require(target.profile == null && target.items.isEmpty() && target.skills.isEmpty() && target.documents.isEmpty()) {
            "The account workspace is not empty. To avoid overwriting records, local data can only be moved into an empty account workspace."
        }
        val guest = snapshot(WorkspaceDatabases.LOCAL)
        require(guest.profile != null || guest.items.isNotEmpty() || guest.skills.isNotEmpty() || guest.documents.isNotEmpty()) { "The local workspace is empty." }
        val folder = File(context.filesDir, "documents/${WorkspaceDatabases.folder(key)}").apply { mkdirs() }
        val copied = mutableListOf<File>()
        try {
            val documents = guest.documents.map { doc ->
                val original = File(doc.localPath)
                if (doc.localPath.isNotBlank() && original.isFile) {
                    val dest = File(folder, "${UUID.randomUUID()}.${original.extension}")
                    copied += dest
                    original.copyTo(dest)
                    doc.copy(localPath = dest.absolutePath)
                } else doc.copy(localPath = "")
            }
            checkWorkspace(key)
            targetDb.withTransaction {
                guest.profile?.let { targetDb.profileDao().upsert(it) }
                targetDb.documentDao().upsertAll(documents)
                targetDb.careerItemDao().upsertAll(guest.items)
                targetDb.skillDao().insertAll(guest.skills)
            }
        } catch (e: Exception) { copied.forEach { it.delete() }; throw e }
        // Clear the guest source only after the destination transaction has succeeded.
        guestDb.withTransaction { guestDb.documentDao().clearAll(); guestDb.careerItemDao().clearAll(); guestDb.skillDao().clearAll(); guestDb.profileDao().clearAll() }
        guest.documents.filter { it.localPath.isNotBlank() }.forEach { File(it.localPath).delete() }
        File(context.filesDir, "exports/local").deleteRecursively()
    }

    private fun validLink(value: String): String = if (value.isBlank()) "" else requireNotNull(UrlPolicy.normalize(value)) { "Use a valid http(s) website URL or DOI; links with unsupported schemes are not accepted." }
    private fun fileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(8192); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    companion object { private const val MAX_FILE_BYTES = 50L * 1024L * 1024L }
}
