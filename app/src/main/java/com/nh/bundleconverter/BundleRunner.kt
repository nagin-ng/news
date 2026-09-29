package com.nh.bundleconverter

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

object BundleRunner {
    private const val MAIN = "com.android.tools.build.bundletool.BundleToolMain"

    private fun prepare(ctx: Context): File {
        val f = File(ctx.filesDir, "bundletool.zip")
        val size = ctx.assets.openFd("bundletool.zip").use { it.length }
        if (!f.exists() || f.length() != size) {
            f.delete()
            ctx.assets.open("bundletool.zip").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        }
        f.setReadOnly() // Android 14+ requires read-only dex
        return f
    }

    /** Runs bundletool main(args) and returns captured stdout/stderr. */
    fun run(ctx: Context, args: List<String>): String {
        val bos = ByteArrayOutputStream()
        val ps = PrintStream(bos, true)
        val oldOut = System.out
        val oldErr = System.err
        var error: Throwable? = null
        try {
            val zip = prepare(ctx)
            val opt = File(ctx.codeCacheDir, "odex").apply { mkdirs() }
            val loader = DexClassLoader(zip.absolutePath, opt.absolutePath, null, ctx.classLoader)
            val m = loader.loadClass(MAIN).getMethod("main", Array<String>::class.java)
            System.setOut(ps); System.setErr(ps)
            val t = Thread(null, Runnable {
                try { m.invoke(null, args.toTypedArray() as Any) }
                catch (e: Throwable) { error = e.cause ?: e }
            }, "bundletool", 256L * 1024 * 1024)
            t.start(); t.join()
        } catch (e: Throwable) {
            error = e
        } finally {
            System.setOut(oldOut); System.setErr(oldErr)
        }
        val out = bos.toString()
        return if (error != null) out + "\nERROR:\n" + Log.getStackTraceString(error) else out
    }
}
