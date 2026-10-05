package com.nh.githubuploader.data

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubException(message: String, val code: Int = 0) : Exception(message)

data class RepoInfo(val defaultBranch: String, val htmlUrl: String)

/** Tree entry: ya to `sha` (blob) ya `content` (text file seedha tree me). */
data class TreeEntry(
    val path: String,
    val mode: String,
    val sha: String? = null,
    val content: String? = null
)

data class RunInfo(
    val id: Long,
    val title: String,
    val status: String,
    val conclusion: String,
    val branch: String,
    val createdAt: String,
    val url: String
)

internal data class ApiResult(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
    fun json(): JSONObject = JSONObject(body)
}

/** GitHub REST API ka chhota client (OkHttp + org.json). */
class GitHubApi(private val token: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun call(method: String, path: String, body: JSONObject? = null): ApiResult {
        val payload = (body?.toString() ?: "").toRequestBody(jsonType)
        val builder = Request.Builder()
            .url("https://api.github.com$path")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GitHubUploader-Android")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(payload)
            "PATCH" -> builder.patch(payload)
            "PUT" -> builder.put(payload)
            else -> throw IllegalArgumentException("Bad method: $method")
        }
        val result = try {
            client.newCall(builder.build()).execute().use { resp ->
                ApiResult(resp.code, resp.body?.string() ?: "")
            }
        } catch (e: IOException) {
            throw GitHubException("Network error: ${e.message}")
        }
        return result
    }

    private fun fail(res: ApiResult, what: String): Nothing {
        val msg = try {
            JSONObject(res.body).optString("message", "")
        } catch (e: Exception) {
            res.body.take(200)
        }
        var hint = when (res.code) {
            401 -> " | Token galat ya expire ho gaya hai."
            403 -> " | Permission ya rate-limit. Token scopes (repo, workflow) check karo."
            404 -> " | Nahi mila: naam ya token ka access check karo."
            422 -> " | GitHub ne data reject kiya."
            else -> ""
        }
        if (res.body.contains("workflow", ignoreCase = true) &&
            res.body.contains("scope", ignoreCase = true)
        ) {
            hint += " | Token me 'workflow' permission add karo."
        }
        throw GitHubException("$what: HTTP ${res.code} $msg$hint", res.code)
    }

    /** Token verify: username (login) return karta hai. */
    fun getUser(): String {
        val r = call("GET", "/user")
        if (!r.ok) fail(r, "Token check")
        return r.json().getString("login")
    }

    /** Repo ki info; repo na ho to null. */
    fun getRepo(owner: String, repo: String): RepoInfo? {
        val r = call("GET", "/repos/$owner/$repo")
        if (r.code == 404) return null
        if (!r.ok) fail(r, "Repo check")
        val j = r.json()
        return RepoInfo(
            defaultBranch = j.optString("default_branch", "main"),
            htmlUrl = j.optString("html_url", "https://github.com/$owner/$repo")
        )
    }

    fun createRepo(owner: String, login: String, name: String, isPrivate: Boolean): RepoInfo {
        val body = JSONObject()
            .put("name", name)
            .put("private", isPrivate)
            .put("auto_init", true)
            .put("description", "Created with GitHub Uploader")
        val path = if (owner.equals(login, ignoreCase = true)) "/user/repos" else "/orgs/$owner/repos"
        val r = call("POST", path, body)
        if (!r.ok) fail(r, "Repo banana")
        val j = r.json()
        return RepoInfo(
            defaultBranch = j.optString("default_branch", "main"),
            htmlUrl = j.optString("html_url", "https://github.com/$owner/$name")
        )
    }

    /** Branch ka latest commit sha; branch na ho (ya repo khali ho) to null. */
    fun getBranchSha(owner: String, repo: String, branch: String): String? {
        val r = call("GET", "/repos/$owner/$repo/git/ref/heads/$branch")
        if (r.code == 404 || r.code == 409) return null
        if (!r.ok) fail(r, "Branch check")
        return r.json().getJSONObject("object").getString("sha")
    }

    /** Bilkul khali repo me pehla commit banata hai (Git Data API ke liye zaroori). */
    fun bootstrap(owner: String, repo: String) {
        val body = JSONObject()
            .put("message", "Initial commit")
            .put("content", Base64.encodeToString("# $repo\n".toByteArray(), Base64.NO_WRAP))
        val r = call("PUT", "/repos/$owner/$repo/contents/README.md", body)
        if (!r.ok) fail(r, "Initial commit")
    }

    fun getCommitTreeSha(owner: String, repo: String, commitSha: String): String {
        val r = call("GET", "/repos/$owner/$repo/git/commits/$commitSha")
        if (!r.ok) fail(r, "Commit padhna")
        return r.json().getJSONObject("tree").getString("sha")
    }

    fun createBlob(owner: String, repo: String, bytes: ByteArray): String {
        val body = JSONObject()
            .put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
            .put("encoding", "base64")
        val r = call("POST", "/repos/$owner/$repo/git/blobs", body)
        if (!r.ok) fail(r, "File upload (blob)")
        return r.json().getString("sha")
    }

    fun createTree(owner: String, repo: String, baseTree: String?, entries: List<TreeEntry>): String {
        val arr = JSONArray()
        for (e in entries) {
            val o = JSONObject()
                .put("path", e.path)
                .put("mode", e.mode)
                .put("type", "blob")
            if (e.sha != null) o.put("sha", e.sha) else o.put("content", e.content ?: "")
            arr.put(o)
        }
        val body = JSONObject().put("tree", arr)
        if (baseTree != null) body.put("base_tree", baseTree)
        val r = call("POST", "/repos/$owner/$repo/git/trees", body)
        if (!r.ok) fail(r, "Tree banana")
        return r.json().getString("sha")
    }

    fun createCommit(owner: String, repo: String, message: String, tree: String, parent: String): String {
        val body = JSONObject()
            .put("message", message)
            .put("tree", tree)
            .put("parents", JSONArray().put(parent))
        val r = call("POST", "/repos/$owner/$repo/git/commits", body)
        if (!r.ok) fail(r, "Commit banana")
        return r.json().getString("sha")
    }

    fun updateRef(owner: String, repo: String, branch: String, sha: String) {
        val body = JSONObject().put("sha", sha).put("force", false)
        val r = call("PATCH", "/repos/$owner/$repo/git/refs/heads/$branch", body)
        if (!r.ok) fail(r, "Branch update")
    }

    fun createRef(owner: String, repo: String, branch: String, sha: String) {
        val body = JSONObject().put("ref", "refs/heads/$branch").put("sha", sha)
        val r = call("POST", "/repos/$owner/$repo/git/refs", body)
        if (!r.ok) fail(r, "Naya branch banana")
    }

    fun listRuns(owner: String, repo: String): List<RunInfo> {
        val r = call("GET", "/repos/$owner/$repo/actions/runs?per_page=10")
        if (!r.ok) fail(r, "Builds list")
        val arr = r.json().getJSONArray("workflow_runs")
        val out = ArrayList<RunInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                RunInfo(
                    id = o.getLong("id"),
                    title = o.optString("display_title", o.optString("name", "")),
                    status = o.optString("status", ""),
                    conclusion = if (o.isNull("conclusion")) "" else o.optString("conclusion", ""),
                    branch = o.optString("head_branch", ""),
                    createdAt = o.optString("created_at", ""),
                    url = o.optString("html_url", "")
                )
            )
        }
        return out
    }
}
