package com.nh.githubuploader.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nh.githubuploader.data.FolderSource
import com.nh.githubuploader.data.GitHubApi
import com.nh.githubuploader.data.Prefs
import com.nh.githubuploader.data.ProjectScanner
import com.nh.githubuploader.data.ProjectSource
import com.nh.githubuploader.data.RunInfo
import com.nh.githubuploader.data.UploadConfig
import com.nh.githubuploader.data.Uploader
import com.nh.githubuploader.data.ZipSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UploadUiState(
    val running: Boolean = false,
    val progress: Float = 0f,
    val log: List<String> = emptyList(),
    val error: String? = null,
    val commitUrl: String? = null,
    val repoUrl: String? = null,
    val skipped: List<String> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    // ---- Settings / form state (har change turant save hota hai) ----
    var token by mutableStateOf(prefs.token)
        private set
    var login by mutableStateOf(prefs.login)
        private set
    var owner by mutableStateOf(prefs.owner)
        private set
    var repo by mutableStateOf(prefs.repo)
        private set
    var branch by mutableStateOf(prefs.branch)
        private set
    var message by mutableStateOf(prefs.message)
        private set
    var createRepo by mutableStateOf(prefs.createRepo)
        private set
    var privateRepo by mutableStateOf(prefs.privateRepo)
        private set
    var addWorkflow by mutableStateOf(prefs.addWorkflow)
        private set
    var mirror by mutableStateOf(prefs.mirror)
        private set

    fun updateToken(v: String) { token = v.trim(); prefs.token = token }
    fun updateOwner(v: String) { owner = v.trim(); prefs.owner = owner }
    fun updateRepo(v: String) { repo = v.trim(); prefs.repo = repo }
    fun updateBranch(v: String) { branch = v.trim(); prefs.branch = branch }
    fun updateMessage(v: String) { message = v; prefs.message = v }
    fun updateCreateRepo(v: Boolean) { createRepo = v; prefs.createRepo = v }
    fun updatePrivateRepo(v: Boolean) { privateRepo = v; prefs.privateRepo = v }
    fun updateAddWorkflow(v: Boolean) { addWorkflow = v; prefs.addWorkflow = v }
    fun updateMirror(v: Boolean) { mirror = v; prefs.mirror = v }

    // ---- Token verify ----
    var tokenStatus by mutableStateOf<String?>(null)
        private set
    var verifying by mutableStateOf(false)
        private set

    fun verifyToken() {
        if (token.isBlank()) {
            tokenStatus = "Token khali hai."
            return
        }
        verifying = true
        tokenStatus = "Check ho raha hai..."
        val t = token
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = GitHubApi(t).getUser()
                prefs.login = user
                login = user
                if (owner.isBlank()) {
                    owner = user
                    prefs.owner = user
                }
                tokenStatus = "Token sahi hai. Account: @$user"
            } catch (e: Throwable) {
                tokenStatus = e.message ?: e.toString()
            }
            verifying = false
        }
    }

    // ---- Project source (ZIP / folder) ----
    private var source: ProjectSource? = null
    var sourceLabel by mutableStateOf<String?>(null)
        private set
    var sourceInfo by mutableStateOf("")
        private set

    fun pickZip(uri: Uri) {
        val name = displayName(uri) ?: "project.zip"
        setSource(ZipSource(getApplication<Application>().contentResolver, uri, name))
    }

    fun pickFolder(uri: Uri) {
        val app = getApplication<Application>()
        val name = DocumentFile.fromTreeUri(app, uri)?.name ?: "folder"
        setSource(FolderSource(app, uri, name))
    }

    private fun setSource(src: ProjectSource) {
        source = src
        sourceLabel = src.label
        sourceInfo = "Scan ho raha hai..."
        viewModelScope.launch(Dispatchers.IO) {
            sourceInfo = try {
                val scan = ProjectScanner.scan(src.listPaths())
                var info = "${scan.entries.size} files upload hongi, ${scan.ignoredCount} skip (build/keystore/etc)."
                if (!scan.hasSettings) info += "\nWarning: settings.gradle nahi mila, shayad ye Android project nahi hai."
                if (scan.hasWorkflow) info += "\nWorkflow pehle se project me hai."
                info
            } catch (e: Throwable) {
                "Scan error: ${e.message ?: e.toString()}"
            }
        }
    }

    private fun displayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---- Upload ----
    private val upload = MutableStateFlow(UploadUiState())
    val uploadState: StateFlow<UploadUiState> = upload.asStateFlow()

    fun startUpload() {
        val src = source
        val problem = when {
            upload.value.running -> return
            token.isBlank() -> "Pehle Settings me GitHub token daalo."
            src == null -> "Pehle ZIP file ya folder chuno."
            repo.isBlank() -> "Repo ka naam daalo."
            else -> null
        }
        if (problem != null || src == null) {
            upload.value = UploadUiState(error = problem)
            return
        }

        upload.value = UploadUiState(running = true, log = listOf("Shuru ho raha hai..."))
        val cfg = UploadConfig(
            token = token,
            owner = owner,
            repo = repo,
            branch = branch,
            message = message,
            createRepoIfMissing = createRepo,
            privateRepo = privateRepo,
            addWorkflow = addWorkflow,
            mirror = mirror
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val yaml = getApplication<Application>().assets.open("android-build.yml")
                    .bufferedReader().use { it.readText() }
                val result = Uploader(
                    cfg = cfg,
                    source = src,
                    workflowYaml = yaml,
                    log = { line -> upload.update { s -> s.copy(log = s.log + line) } },
                    progress = { p -> upload.update { s -> s.copy(progress = p) } }
                ).run()
                if (owner.isBlank()) {
                    owner = result.owner
                    prefs.owner = result.owner
                }
                upload.update { s ->
                    s.copy(
                        running = false,
                        progress = 1f,
                        commitUrl = result.commitUrl,
                        repoUrl = result.repoUrl,
                        skipped = result.skipped
                    )
                }
            } catch (e: Throwable) {
                upload.update { s -> s.copy(running = false, error = e.message ?: e.toString()) }
            }
        }
    }

    // ---- Builds (GitHub Actions runs) ----
    var runs by mutableStateOf<List<RunInfo>>(emptyList())
        private set
    var runsLoading by mutableStateOf(false)
        private set
    var runsError by mutableStateOf<String?>(null)
        private set

    fun loadRuns() {
        if (token.isBlank() || repo.isBlank()) {
            runsError = "Settings me token aur Upload tab me repo ka naam daalo."
            return
        }
        runsLoading = true
        runsError = null
        val t = token
        val r = repo
        val o = owner.ifBlank { login }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = GitHubApi(t)
                val who = o.ifBlank { api.getUser() }
                runs = api.listRuns(who, r)
            } catch (e: Throwable) {
                runsError = e.message ?: e.toString()
            }
            runsLoading = false
        }
    }

    /** https://github.com/<owner>/<repo> (owner pata na ho to null) */
    fun repoWebUrl(): String? {
        val who = owner.ifBlank { login }
        return if (who.isBlank() || repo.isBlank()) null else "https://github.com/$who/$repo"
    }
}
