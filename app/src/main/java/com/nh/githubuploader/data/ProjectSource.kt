package com.nh.githubuploader.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/** Project ka source: ZIP file ya phone ka folder. */
interface ProjectSource {
    val label: String

    /** Saari file paths (folders nahi). */
    fun listPaths(): List<String>

    /** `accept` jin paths ke liye true de, unki bytes `onFile` ko milti hain (ek-ek karke, memory bachane ke liye). */
    fun readFiles(accept: (String) -> Boolean, onFile: (String, ByteArray) -> Unit)
}

class ZipSource(
    private val resolver: ContentResolver,
    private val uri: Uri,
    override val label: String
) : ProjectSource {

    private fun openZip(): ZipInputStream {
        val stream = resolver.openInputStream(uri) ?: throw IOException("File khul nahi rahi")
        return ZipInputStream(BufferedInputStream(stream))
    }

    private fun normalize(name: String): String = name.replace('\\', '/').trimStart('/')

    override fun listPaths(): List<String> {
        val out = ArrayList<String>()
        openZip().use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) out.add(normalize(entry.name))
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return out
    }

    override fun readFiles(accept: (String) -> Boolean, onFile: (String, ByteArray) -> Unit) {
        openZip().use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val path = normalize(entry.name)
                    if (accept(path)) onFile(path, zis.readBytes())
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}

class FolderSource(
    private val context: Context,
    private val treeUri: Uri,
    override val label: String
) : ProjectSource {

    private val files: Map<String, Uri> by lazy {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: throw IOException("Folder khul nahi raha")
        val out = LinkedHashMap<String, Uri>()
        walk(root, "", out)
        out
    }

    private fun walk(dir: DocumentFile, prefix: String, out: MutableMap<String, Uri>) {
        val children = dir.listFiles()
        val isGradleDir = children.any { c ->
            val n = c.name
            n == "build.gradle" || n == "build.gradle.kts"
        }
        for (child in children) {
            val name = child.name ?: continue
            if (child.isDirectory) {
                // Bade/bekaar folders ko pehle hi skip karo (SAF listing slow hoti hai)
                if (name in IgnoreRules.SKIP_DIRS) continue
                if (name == "build" && isGradleDir) continue
                walk(child, "$prefix$name/", out)
            } else if (child.isFile) {
                out[prefix + name] = child.uri
            }
        }
    }

    override fun listPaths(): List<String> = files.keys.toList()

    override fun readFiles(accept: (String) -> Boolean, onFile: (String, ByteArray) -> Unit) {
        for ((path, uri) in files) {
            if (!accept(path)) continue
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
            onFile(path, bytes)
        }
    }
}
