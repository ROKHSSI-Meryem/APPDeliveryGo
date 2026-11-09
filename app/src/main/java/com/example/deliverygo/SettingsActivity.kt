package com.example.deliverygo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupNavigation(R.id.nav_settings)

        findViewById<Button>(R.id.btnLanguage).setOnClickListener { showLanguageDialog() }

        findViewById<Button>(R.id.btnTestNotification).setOnClickListener {
            NotificationHelper.createChannel(this)
            NotificationHelper.requestPermission(this)
            NotificationHelper.show(
                this,
                getString(R.string.notification_title),
                getString(R.string.notification_message)
            )
        }
    }

    private fun showLanguageDialog() {
        val names = arrayOf("Français", "العربية", "English")
        val codes = arrayOf("fr", "ar", "en")

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_language)
            .setItems(names) { _, which ->
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(codes[which])
                )
            }
            .show()
    }
}