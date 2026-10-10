package com.aryanpaode.portfolioforgeai.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot
import com.aryanpaode.portfolioforgeai.di.AppContainer
import com.aryanpaode.portfolioforgeai.service.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.localRepository
    val authState = container.cloudService.state
    val data = combine(repo.data, authState) { snapshot, account ->
        if (snapshot.workspaceId == account.workspaceId) snapshot else Snapshot.empty(account.workspaceId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Snapshot.empty(authState.value.workspaceId))
    val profile = data.map { it.profile }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val documents = data.map { it.documents }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val careerItems = data.map { it.items }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val skills = data.map { it.skills }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val readiness = data.map { container.readinessEngine.calculate(it.profile, it.items, it.skills, it.documents) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReadinessResult(0, listOf("Loading workspace…")))
    val networkState = container.networkMonitor.state
    private val _busy = MutableStateFlow(false); val busy: StateFlow<Boolean> = _busy
    private val _busyText = MutableStateFlow(""); val busyText: StateFlow<String> = _busyText
    private val _message = MutableStateFlow<String?>(null); val message: StateFlow<String?> = _message
    private val _pdfPath = MutableStateFlow<String?>(null); val pdfPath: StateFlow<String?> = _pdfPath
    private val _htmlPath = MutableStateFlow<String?>(null); val htmlPath: StateFlow<String?> = _htmlPath
    private val _publishedLink = MutableStateFlow<String?>(null); val publishedLink: StateFlow<String?> = _publishedLink
    val cloudConfigured get() = authState.value.configured
    val signedIn get() = authState.value.signedIn
    val signedInEmail get() = authState.value.email

    init {
        viewModelScope.launch {
            authState.map { it.workspaceId }.distinctUntilChanged().collect {
                _pdfPath.value = null; _htmlPath.value = null; _publishedLink.value = null
            }
        }
        // Any content change invalidates share buttons so an old export cannot be mistaken for current data.
        viewModelScope.launch {
            data.filter { it.loaded }.distinctUntilChanged().collect {
                _pdfPath.value = null; _htmlPath.value = null
            }
        }
    }
    fun clearMessage() { _message.value = null }
    fun showMessage(value: String) { _message.value = value }

    fun saveProfile(profile: ProfileEntity) = task("Saving profile") { repo.saveProfile(profile); "Profile saved." }
    fun saveCareerItem(item: CareerItemEntity, onSuccess: () -> Unit = {}) = task("Saving entry") {
        repo.addCareerItem(item); onSuccess(); "${CareerType.heading(item.type)} entry saved."
    }
    fun deleteCareerItem(item: CareerItemEntity) = task("Removing entry") { repo.deleteCareerItem(item); "Entry removed. Its evidence document was kept." }
    fun addSkill(name: String) = task("Saving skill") { repo.addSkill(name); "Skill saved (duplicates are merged)." }
    fun deleteSkill(skill: SkillEntity) = task("Removing skill") { repo.deleteSkill(skill); "Skill removed." }

    fun importDocument(uri: Uri, hint: String = "Auto detect", onSuccess: (String) -> Unit = {}) = task("Reading document — please wait") {
        val result = repo.importDocument(uri, hint)
        onSuccess(result.document.id)
        if (result.duplicate) "This document already exists. Review it to update the same entries without duplicating them."
        else if (result.document.extractedText.isBlank()) "File saved, but no readable text was found. Add or correct details in review."
        else "Document read. Review the extracted sections and skills before saving."
    }
    fun rereadDocument(id: String) = task("Re-reading every page with local OCR") { repo.rereadDocument(id); "Draft text updated. Review it before saving; existing career entries were kept." }
    fun deleteDocument(id: String) = task("Removing evidence") { repo.deleteDocument(id); "Evidence removed. Approved career entries and skills were kept." }

    fun previewDocument(id: String, text: String, fileName: String, category: String): DocumentReview =
        container.documentExtraction.parse(id, text, fileName, category)

    fun saveDocumentReview(review: DocumentReview, items: List<CareerItemEntity>, skills: List<SkillEntity>, profile: ProfileEntity?, onSuccess: () -> Unit) = task("Saving verified information") {
        val result = repo.saveReviewedDocument(review, items, skills, profile)
        onSuccess()
        "Saved: ${result.added} new entries, ${result.updated} updated, ${result.newSkills} new skills. ${result.duplicateEntries} existing entries kept."
    }

    fun signIn(email: String, password: String) = onlineTask("Signing in") { container.cloudService.signIn(email, password) }
    fun register(email: String, password: String) = onlineTask("Creating account") { container.cloudService.register(email, password) }
    fun resetPassword(email: String) = onlineTask("Sending password-reset email") { container.cloudService.resetPassword(email) }
    fun sendVerification() = onlineTask("Sending verification email") { container.cloudService.sendVerification() }
    fun refreshUser() = onlineTask("Checking email verification") { container.cloudService.refreshUser() }
    fun signOut() {
        if (_busy.value) return
        _pdfPath.value = null; _htmlPath.value = null; _publishedLink.value = null
        container.cloudService.signOut()
        showMessage("Signed out. The account workspace is hidden; local mode is a separate workspace.")
    }
    fun moveLocalToAccount() = task("Moving local data to this account") {
        repo.moveLocalWorkspaceToAccount(); "Local data moved to this account. The signed-out local workspace is now empty."
    }
    fun syncCloud() = onlineTask("Saving private cloud snapshot") {
        container.cloudService.sync(repo.snapshot()); "Private cloud snapshot saved. Evidence files remain on this device."
    }
    fun restoreCloud() = onlineTask("Restoring and merging cloud data") {
        repo.restoreStructuredData(container.cloudService.fetchStructuredData()); "Cloud data merged. Existing local entries and profile values were kept."
    }
    fun publishPortfolio(includeEmail: Boolean = false) = onlineTask("Publishing portfolio") {
        val key = repo.workspaceId
        val link = container.cloudService.publishPortfolio(repo.snapshot(), includeEmail)
        if (repo.workspaceId == key) _publishedLink.value = link
        "Portfolio data published. The HTTPS link also requires the included Firebase Hosting site to be deployed."
    }
    fun unpublishPortfolio() = onlineTask("Unpublishing portfolio") {
        container.cloudService.unpublishPortfolio(); _publishedLink.value = null; "Public portfolio removed from Firestore. Previously shared files are not recalled."
    }
    fun revealPortfolioLink() { _publishedLink.value = container.cloudService.portfolioLink() }

    fun generateResumePdf() = task("Generating resume PDF") {
        val snapshot = repo.snapshot()
        val file = withContext(Dispatchers.IO) { container.resumePdfGenerator.generate(snapshot) }
        if (snapshot.workspaceId == repo.workspaceId) _pdfPath.value = file.absolutePath
        "Resume PDF generated. Choose WhatsApp, Save to device, or More apps."
    }
    fun generatePortfolioHtml() = task("Generating portfolio HTML") {
        val snapshot = repo.snapshot()
        val file = withContext(Dispatchers.IO) { container.portfolioHtmlGenerator.generate(snapshot) }
        if (snapshot.workspaceId == repo.workspaceId) _htmlPath.value = file.absolutePath
        "Portfolio HTML generated. This is a file, not yet a public website link."
    }

    private fun onlineTask(label: String, block: suspend () -> String) {
        if (!networkState.value.online) { showMessage("Connect to the internet for this Firebase action. Local editing and file exports still work offline."); return }
        task(label) { withTimeout(45_000L) { block() } }
    }
    private fun task(label: String, block: suspend () -> String) {
        if (_busy.value) return
        _busy.value = true; _busyText.value = label
        viewModelScope.launch {
            try { _message.value = block() }
            catch (e: kotlinx.coroutines.TimeoutCancellationException) { _message.value = "The operation timed out. Check connectivity and retry. No success is assumed." }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _message.value = CloudService.friendlyError(e) }
            finally { _busy.value = false; _busyText.value = "" }
        }
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppViewModel::class.java)) { "Unknown ViewModel class." }
        return AppViewModel(container) as T
    }
}
