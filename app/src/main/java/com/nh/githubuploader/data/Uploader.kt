package com.nh.githubuploader.data

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

class UploadConfig(
    val token: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val message: String,
    val createRepoIfMissing: Boolean,
    val privateRepo: Boolean,
    val addWorkflow: Boolean,
    val mirror: Boolean
)

class UploadResult(
    val login: String,
    val owner: String,
    val repoUrl: String,
    val commitUrl: String,
    val fileCount: Int,
    val skipped: List<String>
)

/**
 * Poora project ek hi commit me upload karta hai (Git Data API):
 * text files seedha tree me, binary files blob bana kar.
 */
class Uploader(
    private val cfg: UploadConfig,
    private val source: ProjectSource,
    private val workflowYaml: String,
    private val log: (String) -> Unit,
    private val progress: (Float) -> Unit
) {
    private val api = GitHubApi(cfg.token)

    fun run(): UploadResult {
        log("Token check ho raha hai...")
        val login = api.getUser()
        val owner = cfg.owner.ifBlank { login }
        val repo = cfg.repo

        val repoInfo = api.getRepo(owner, repo) ?: run {
            if (!cfg.createRepoIfMissing) {
                throw GitHubException("Repo '$owner/$repo' nahi mila. 'Repo na ho to banao' ON karo ya naam sahi karo.")
            }
            log("Naya repo ban raha hai: $owner/$repo")
            api.createRepo(owner, login, repo, cfg.privateRepo)
        }
        progress(0.05f)

        val branch = cfg.branch.ifBlank { repoInfo.defaultBranch }
        val (parentSha, branchExists) = resolveParent(owner, repo, branch, repoInfo.defaultBranch)
        progress(0.1f)

        log("Files scan ho rahi hain...")
        val scan = ProjectScanner.scan(source.listPaths())
        if (scan.entries.isEmpty()) throw GitHubException("Upload karne layak koi file nahi mili.")
        if (!scan.hasSettings) {
            log("Warning: root me settings.gradle nahi mila. Build fail ho sakta hai (ZIP/folder ka structure check karo).")
        }
        log("${scan.entries.size} files upload hongi, ${scan.ignoredCount} skip (build/keystore/etc).")

        val needWorkflow = cfg.addWorkflow && !scan.hasWorkflow
        val total = scan.entries.size + (if (needWorkflow) 1 else 0)

        val baseTree = api.getCommitTreeSha(owner, repo, parentSha)
        var currentTree: String? = if (cfg.mirror) null else baseTree
        val chunk = ArrayList<TreeEntry>()
        var chunkSize = 0
        val skipped = ArrayList<String>()
        var emptyBlob: String? = null
        var done = 0

        fun flush() {
            if (chunk.isEmpty()) return
            currentTree = api.createTree(owner, repo, currentTree, chunk)
            chunk.clear()
            chunkSize = 0
        }

        fun add(entry: TreeEntry, size: Int) {
            chunk.add(entry)
            chunkSize += size
            if (chunk.size >= 150 || chunkSize >= 2_500_000) flush()
        }

        source.readFiles(
            accept = { path -> scan.entries.containsKey(path) },
            onFile = { path, bytes ->
                val rel = scan.entries.getValue(path)
                if (bytes.size > MAX_FILE_BYTES) {
                    skipped.add("$rel (${bytes.size / 1_048_576} MB, bahut badi)")
                } else {
                    val mode = modeFor(rel)
                    val text = if (bytes.isNotEmpty() && bytes.size <= INLINE_LIMIT) decodeUtf8OrNull(bytes) else null
                    if (text != null) {
                        add(TreeEntry(rel, mode, content = text), text.length)
                    } else {
                        val sha = if (bytes.isEmpty()) {
                            emptyBlob ?: api.createBlob(owner, repo, bytes).also { emptyBlob = it }
                        } else {
                            api.createBlob(owner, repo, bytes)
                        }
                        add(TreeEntry(rel, mode, sha = sha), 100)
                    }
                }
                done++
                if (done % 25 == 0) log("$done / $total files ready...")
                progress(0.1f + 0.6f * done / total)
            }
        )

        if (needWorkflow) {
            add(TreeEntry(".github/workflows/android-build.yml", "100644", content = workflowYaml), workflowYaml.length)
            log("Build workflow add kiya (.github/workflows/android-build.yml).")
        } else if (cfg.addWorkflow) {
            log("Workflow pehle se maujood hai, naya add nahi kiya.")
        }
        flush()
        progress(0.75f)

        val finalTree = currentTree ?: throw GitHubException("Koi file upload nahi ho paayi.")
        log("Commit ban raha hai...")
        val message = cfg.message.ifBlank { "Upload from GitHub Uploader" }
        val commitSha = api.createCommit(owner, repo, message, finalTree, parentSha)
        if (branchExists) {
            api.updateRef(owner, repo, branch, commitSha)
        } else {
            api.createRef(owner, repo, branch, commitSha)
        }
        progress(1f)
        log("Done! Commit ${commitSha.take(7)} branch '$branch' par push ho gaya.")

        val repoUrl = repoInfo.htmlUrl.ifBlank { "https://github.com/$owner/$repo" }
        return UploadResult(
            login = login,
            owner = owner,
            repoUrl = repoUrl,
            commitUrl = "$repoUrl/commit/$commitSha",
            fileCount = total - skipped.size,
            skipped = skipped
        )
    }

    /** Parent commit sha + kya branch pehle se exist karta hai. Khali repo ho to initial commit banata hai. */
    private fun resolveParent(
        owner: String,
        repo: String,
        branch: String,
        defaultBranch: String
    ): Pair<String, Boolean> {
        val existing = api.getBranchSha(owner, repo, branch)
        if (existing != null) return Pair(existing, true)

        val defaultSha: String = api.getBranchSha(owner, repo, defaultBranch) ?: run {
            log("Repo khali hai, initial commit bana raha hoon...")
            api.bootstrap(owner, repo)
            api.getBranchSha(owner, repo, defaultBranch)
                ?: throw GitHubException("Khali repo me initial commit fail ho gaya.")
        }
        return Pair(defaultSha, branch == defaultBranch)
    }

    private fun modeFor(path: String): String =
        if (path == "gradlew" || path.endsWith("/gradlew") || path.endsWith(".sh")) "100755" else "100644"

    private fun decodeUtf8OrNull(bytes: ByteArray): String? {
        if (bytes.any { b -> b == 0.toByte() }) return null
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            null
        }
    }

    companion object {
        private const val MAX_FILE_BYTES = 25 * 1024 * 1024
        private const val INLINE_LIMIT = 400_000
    }
}
