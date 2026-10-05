package com.nh.githubuploader.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Token encrypted storage me rehta hai (agar device support kare),
 * baaki settings normal private SharedPreferences me.
 */
class Prefs(context: Context) {

    private val secure: SharedPreferences = try {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "uploader_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("uploader_secure_fallback", Context.MODE_PRIVATE)
    }

    private val plain: SharedPreferences =
        context.getSharedPreferences("uploader_settings", Context.MODE_PRIVATE)

    var token: String
        get() = secure.getString("token", "") ?: ""
        set(value) {
            secure.edit().putString("token", value).apply()
        }

    var login: String
        get() = plain.getString("login", "") ?: ""
        set(value) {
            plain.edit().putString("login", value).apply()
        }

    var owner: String
        get() = plain.getString("owner", "") ?: ""
        set(value) {
            plain.edit().putString("owner", value).apply()
        }

    var repo: String
        get() = plain.getString("repo", "") ?: ""
        set(value) {
            plain.edit().putString("repo", value).apply()
        }

    var branch: String
        get() = plain.getString("branch", "main") ?: "main"
        set(value) {
            plain.edit().putString("branch", value).apply()
        }

    var message: String
        get() = plain.getString("message", "Upload from GitHub Uploader") ?: ""
        set(value) {
            plain.edit().putString("message", value).apply()
        }

    var createRepo: Boolean
        get() = plain.getBoolean("createRepo", true)
        set(value) {
            plain.edit().putBoolean("createRepo", value).apply()
        }

    var privateRepo: Boolean
        get() = plain.getBoolean("privateRepo", true)
        set(value) {
            plain.edit().putBoolean("privateRepo", value).apply()
        }

    var addWorkflow: Boolean
        get() = plain.getBoolean("addWorkflow", true)
        set(value) {
            plain.edit().putBoolean("addWorkflow", value).apply()
        }

    var mirror: Boolean
        get() = plain.getBoolean("mirror", false)
        set(value) {
            plain.edit().putBoolean("mirror", value).apply()
        }
}
