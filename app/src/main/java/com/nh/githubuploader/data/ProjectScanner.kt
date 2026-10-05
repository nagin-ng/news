package com.nh.githubuploader.data

/** Kaun si files/folders upload nahi karni (build output, keystore, IDE files...). */
class IgnoreRules(allPaths: List<String>) {

    // Un folders ki list jinme build.gradle(.kts) hai: unke andar ka "build/" folder build output hai.
    private val gradleDirs: Set<String> = allPaths
        .filter { p ->
            p == "build.gradle" || p == "build.gradle.kts" ||
                p.endsWith("/build.gradle") || p.endsWith("/build.gradle.kts")
        }
        .map { p -> p.substringBeforeLast('/', "") }
        .toSet()

    fun ignored(path: String): Boolean {
        val segments = path.split('/')
        val name = segments.last()
        if (segments.any { s -> s in SKIP_DIRS }) return true
        if (name in SKIP_FILES) return true
        if (name.endsWith(".iml") || name.endsWith(".hprof")) return true
        if (name.endsWith(".apk") || name.endsWith(".aab") || name.endsWith(".ap_")) return true
        if ((name.endsWith(".jks") || name.endsWith(".keystore")) && name != "debug.keystore") return true
        for (d in gradleDirs) {
            val prefix = if (d.isEmpty()) "build/" else "$d/build/"
            if (path.startsWith(prefix)) return true
        }
        return false
    }

    companion object {
        val SKIP_DIRS: Set<String> = setOf(
            ".git", ".gradle", ".idea", ".cxx", ".externalNativeBuild", "__MACOSX"
        )
        val SKIP_FILES: Set<String> = setOf(".DS_Store", "local.properties", "Thumbs.db")
    }
}

class ScanResult(
    /** original path -> repo me jaane wala relative path */
    val entries: Map<String, String>,
    val ignoredCount: Int,
    val hasSettings: Boolean,
    val hasWorkflow: Boolean
)

object ProjectScanner {

    fun scan(allPaths: List<String>): ScanResult {
        val rules = IgnoreRules(allPaths)
        val candidates = allPaths.filter { p ->
            !rules.ignored(p) && p.split('/').none { s -> s == ".." || s.isEmpty() }
        }
        val root = commonRoot(candidates)
        val map = LinkedHashMap<String, String>()
        for (p in candidates) {
            val rel = p.removePrefix(root)
            if (rel.isNotEmpty()) map[p] = rel
        }
        val rels = map.values
        return ScanResult(
            entries = map,
            ignoredCount = allPaths.size - map.size,
            hasSettings = rels.any { r -> r == "settings.gradle" || r == "settings.gradle.kts" },
            hasWorkflow = rels.any { r ->
                r.startsWith(".github/workflows/") && (r.endsWith(".yml") || r.endsWith(".yaml"))
            }
        )
    }

    /** Agar saari files ek hi top-level folder (e.g. "MyApp/") ke andar hain to wo prefix return karta hai. */
    private fun commonRoot(paths: List<String>): String {
        if (paths.isEmpty()) return ""
        val first = paths[0].substringBefore('/', "")
        if (first.isEmpty()) return ""
        val prefix = "$first/"
        return if (paths.all { p -> p.startsWith(prefix) }) prefix else ""
    }
}
