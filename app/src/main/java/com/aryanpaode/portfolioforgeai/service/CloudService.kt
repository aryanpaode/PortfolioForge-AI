package com.aryanpaode.portfolioforgeai.service

import android.content.Context
import com.aryanpaode.portfolioforgeai.data.db.WorkspaceDatabases
import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import com.aryanpaode.portfolioforgeai.data.model.DocumentEntity
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import com.aryanpaode.portfolioforgeai.data.model.SkillEntity
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/** Authentication is observable; Compose updates on login/logout/token refresh immediately. */
data class AuthState(
    val configured: Boolean = false,
    val uid: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false
) {
    val signedIn: Boolean get() = uid != null
    val workspaceId: String get() = uid ?: WorkspaceDatabases.LOCAL
}

data class CloudStructuredData(
    val profile: ProfileEntity?,
    val items: List<CareerItemEntity>,
    val skills: List<SkillEntity>,
    val documents: List<DocumentEntity> = emptyList(),
    val ownerUid: String = ""
)

class CloudService(context: Context) {
    private val app: FirebaseApp? = FirebaseApp.getApps(context).firstOrNull()
        ?: FirebaseApp.initializeApp(context.applicationContext)
    private val auth: FirebaseAuth? = app?.let { FirebaseAuth.getInstance(it) }
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<AuthState> = _state
    private var lastVerificationSent = 0L
    private val authListener = FirebaseAuth.AuthStateListener { _state.value = readState() }
    private val tokenListener = FirebaseAuth.IdTokenListener { _state.value = readState() }
    init { auth?.addAuthStateListener(authListener); auth?.addIdTokenListener(tokenListener) }

    private fun readState() = AuthState(app != null, auth?.currentUser?.uid, auth?.currentUser?.email, auth?.currentUser?.isEmailVerified == true)
    val isConfigured get() = state.value.configured
    val isSignedIn get() = state.value.signedIn
    val currentUserEmail get() = state.value.email

    private fun configuredAuth() = requireNotNull(auth) { "Firebase is not configured. Put your project's google-services.json in app/, then sync and rebuild." }
    private fun validateEmail(email: String) {
        require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())) { "Enter a valid email address." }
    }

    suspend fun register(email: String, password: String): String {
        validateEmail(email)
        require(password.length >= 6) { "Use a password of at least six characters (or your Firebase project's stronger policy)." }
        val account = configuredAuth().createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("The account could not be created.")
        _state.value = readState()
        return try {
            account.sendEmailVerification().await()
            lastVerificationSent = System.currentTimeMillis()
            "Account created. Open the verification email, then use 'I have verified my email'."
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { "Account created, but the verification email could not be sent. Use 'Resend verification email' in Account. ${friendlyError(e)}" }
    }

    suspend fun signIn(email: String, password: String): String {
        validateEmail(email); require(password.isNotEmpty()) { "Enter your password." }
        configuredAuth().signInWithEmailAndPassword(email.trim(), password).await()
        _state.value = readState()
        return if (state.value.emailVerified) "Signed in. Your account workspace is ready." else "Signed in. Verify your email before cloud sync or public publishing."
    }

    suspend fun resetPassword(email: String): String {
        validateEmail(email)
        try { configuredAuth().sendPasswordResetEmail(email.trim()).await() }
        catch (e: FirebaseAuthException) { if (e.errorCode != "ERROR_USER_NOT_FOUND") throw e }
        return "If this address is eligible, a password-reset email has been sent. Check your inbox and spam folder."
    }

    suspend fun sendVerification(): String {
        val user = configuredAuth().currentUser ?: error("Sign in first.")
        require(!user.isEmailVerified) { "This email is already verified." }
        require(System.currentTimeMillis() - lastVerificationSent >= 60_000L) { "Please wait a minute before requesting another verification email." }
        user.sendEmailVerification().await()
        lastVerificationSent = System.currentTimeMillis()
        return "Verification email sent. Check your inbox and spam folder."
    }

    suspend fun refreshUser(): String {
        val user = configuredAuth().currentUser ?: error("Sign in first.")
        try { user.reload().await(); user.getIdToken(true).await() }
        catch (e: FirebaseAuthException) {
            if (e.errorCode in setOf("ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN")) signOut()
            throw e
        }
        _state.value = readState()
        return if (state.value.emailVerified) "Email verified. Cloud sync and publishing are enabled." else "Email is not verified yet. Open the email link, then check again."
    }

    fun signOut() { auth?.signOut(); lastVerificationSent = 0L; _state.value = readState() }

    private suspend fun verifiedUser(): FirebaseUser {
        val user = configuredAuth().currentUser ?: error("Sign in before using the cloud.")
        user.reload().await()
        _state.value = readState()
        require(user.isEmailVerified) { "Verify your email first, then press 'I have verified my email'." }
        user.getIdToken(true).await()
        return user
    }
    private fun firestore() = FirebaseFirestore.getInstance(requireNotNull(app))
    private fun checkOwner(snapshot: Snapshot, user: FirebaseUser) {
        check(snapshot.workspaceId == user.uid && state.value.uid == user.uid) { "The account changed. Retry this action from the current workspace." }
    }

    /** One atomic Firestore document prevents half-restored/deleted career lists. No document binaries or local paths. */
    suspend fun sync(snapshot: Snapshot) {
        val user = verifiedUser(); checkOwner(snapshot, user)
        val payload = mapOf(
            "schemaVersion" to 2,
            "updatedAt" to System.currentTimeMillis(),
            "profile" to snapshot.profile?.toMap(),
            "items" to snapshot.items.map { it.toMap() },
            "skills" to snapshot.skills.map { mapOf("id" to it.id, "name" to it.name, "category" to it.category, "source" to it.source, "evidence" to it.evidence) },
            "documents" to snapshot.documents.map { mapOf(
                "id" to it.id, "fileName" to it.fileName, "mimeType" to it.mimeType, "category" to it.category,
                "verifiedText" to if (it.verified) it.verifiedText else "", "verified" to it.verified,
                "createdAt" to it.createdAt, "contentHash" to it.contentHash, "importedAt" to it.importedAt
            ) }
        )
        require(JSONObject(payload).toString().toByteArray(Charsets.UTF_8).size < 850_000) {
            "This workspace is too large for the atomic cloud snapshot. Keep your local workspace and use PDF/HTML exports; nothing has been uploaded or deleted."
        }
        firestore().collection("users").document(user.uid).collection("private").document("snapshot").set(payload).await()
    }

    suspend fun fetchStructuredData(): CloudStructuredData {
        val user = verifiedUser()
        val root = firestore().collection("users").document(user.uid)
        val stored = root.collection("private").document("snapshot").get(Source.SERVER).await()
        if (stored.exists()) {
            val data = stored.data.orEmpty()
            val profile = (data["profile"] as? Map<*, *>)?.let(::readProfile)
            return CloudStructuredData(profile, maps(data["items"]).map(::readItem), maps(data["skills"]).map(::readSkill),
                maps(data["documents"]).map(::readDocument), user.uid)
        }
        // Read older uploaded projects without forcing the user to recreate their data.
        val p = root.collection("private").document("profile").get(Source.SERVER).await()
        val items = root.collection("careerItems").get(Source.SERVER).await().documents.map { readItem(it.data.orEmpty() + ("id" to it.id)) }
        val skills = root.collection("skills").get(Source.SERVER).await().documents.map { readSkill(it.data.orEmpty() + ("id" to it.id)) }
        val docs = root.collection("documents").get(Source.SERVER).await().documents.map { readDocument(it.data.orEmpty() + ("id" to it.id)) }
        require(p.exists() || items.isNotEmpty() || skills.isNotEmpty() || docs.isNotEmpty()) { "No cloud backup exists for this account. Local data was not changed." }
        return CloudStructuredData(p.data?.let(::readProfile), items, skills, docs, user.uid)
    }

    fun portfolioLink(): String? = if (!isSignedIn || app?.options?.projectId.isNullOrBlank()) null else
        "https://${app!!.options.projectId}.web.app/?u=portfolio-${state.value.uid}"

    suspend fun publishPortfolio(snapshot: Snapshot, includeEmail: Boolean = false): String {
        val user = verifiedUser(); checkOwner(snapshot, user)
        val profile = snapshot.profile ?: error("Complete your profile first.")
        require(profile.name.isNotBlank()) { "Add your name before publishing." }
        val data = profile.toMap().toMutableMap().apply {
            remove("phone") // Phone is private. Email is an explicit publish-time choice.
            if (!includeEmail) remove("email")
            put("ownerUid", user.uid)
            put("skills", snapshot.skills.map { mapOf("name" to it.name, "category" to it.category) })
            put("items", snapshot.items.map { it.toMap().filterKeys { key -> key != "sourceDocumentId" && key != "id" } })
            put("updatedAt", System.currentTimeMillis())
        }
        require(JSONObject(HashMap<Any?, Any?>(data)).toString().toByteArray().size < 850_000) { "The portfolio is too large to publish as one record. Use local exports." }
        firestore().collection("publicPortfolios").document("portfolio-${user.uid}").set(data).await()
        return requireNotNull(portfolioLink())
    }

    suspend fun unpublishPortfolio() {
        val user = verifiedUser()
        firestore().collection("publicPortfolios").document("portfolio-${user.uid}").delete().await()
    }

    companion object {
        fun friendlyError(error: Throwable): String = when (error) {
            is FirebaseNetworkException -> "Firebase could not be reached. Check the device's internet connection and try again."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please wait before trying again."
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> "Email or password is incorrect. Try again or use Forgot password."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "This email cannot be registered. Try signing in or resetting its password."
                "ERROR_WEAK_PASSWORD" -> "The password does not meet your Firebase project's password policy."
                "ERROR_USER_DISABLED" -> "This account is disabled. Contact the Firebase project administrator."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Enable Email/Password in Firebase Authentication → Sign-in method."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait before trying again."
                else -> error.localizedMessage ?: "Authentication failed. Check the Firebase setup."
            }
            else -> error.localizedMessage ?: "The action could not be completed."
        }
    }
}

private fun maps(value: Any?): List<Map<*, *>> = (value as? List<*>)?.mapNotNull { it as? Map<*, *> }.orEmpty()
private fun Map<*, *>.text(key: String): String = this[key] as? String ?: ""
private fun Map<*, *>.number(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
private fun readProfile(m: Map<*, *>) = ProfileEntity(name = m.text("name"), email = m.text("email"), phone = m.text("phone"),
    branch = m.text("branch"), semester = m.text("semester"), cgpa = m.text("cgpa"), githubUrl = m.text("githubUrl"),
    linkedInUrl = m.text("linkedInUrl"), targetRole = m.text("targetRole"), summary = m.text("summary"), portfolioUrl = m.text("portfolioUrl"))
private fun readItem(m: Map<*, *>) = CareerItemEntity(m.text("id"), m.text("type"), m.text("title"), m.text("subtitle"),
    m.text("description"), m.text("date"), m.text("link"), m.text("sourceDocumentId"))
private fun readSkill(m: Map<*, *>) = SkillEntity(m.text("id"), m.text("name"), m.text("category").ifBlank { "Other" }, m.text("source"), m.text("evidence"))
private fun readDocument(m: Map<*, *>) = DocumentEntity(id = m.text("id"), fileName = m.text("fileName"), localPath = "", mimeType = m.text("mimeType"),
    category = m.text("category"), verifiedText = m.text("verifiedText"), verified = m["verified"] == true, createdAt = m.number("createdAt"),
    contentHash = m.text("contentHash"), importedAt = m.number("importedAt"), ocrWarning = "Cloud metadata only. The original file is not stored in Firebase.")
private fun ProfileEntity.toMap(): Map<String, Any> = mapOf("name" to name, "email" to email, "phone" to phone, "branch" to branch,
    "semester" to semester, "cgpa" to cgpa, "githubUrl" to githubUrl, "linkedInUrl" to linkedInUrl, "targetRole" to targetRole, "summary" to summary, "portfolioUrl" to portfolioUrl)
private fun CareerItemEntity.toMap(): Map<String, Any> = mapOf("id" to id, "type" to type, "title" to title, "subtitle" to subtitle,
    "description" to description, "date" to date, "link" to link, "sourceDocumentId" to sourceDocumentId)
