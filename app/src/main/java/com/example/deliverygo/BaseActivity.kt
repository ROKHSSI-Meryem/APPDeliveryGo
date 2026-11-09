package com.example.deliverygo

import android.content.Intent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var checkedItemId: Int = 0

    protected fun setupNavigation(checkedItemId: Int) {
        this.checkedItemId = checkedItemId
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)

        findViewById<View>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Bouton "Déconnexion" fixé en bas du menu (rouge).
        // Si la page n'a pas ce bouton dans son layout, l'item du menu reste utilisé.
        val logoutButton: View? = findViewById(R.id.logoutButton)
        if (logoutButton != null) {
            navView.menu.findItem(R.id.nav_logout)?.isVisible = false
            logoutButton.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.START)
                logout()
            }
        }

        navView.setCheckedItem(checkedItemId)
        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_home -> openPage(MainActivity::class.java)
                R.id.nav_profile -> openPage(ProfileActivity::class.java)
                R.id.nav_settings -> openPage(SettingsActivity::class.java)
                R.id.nav_logout -> logout()
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::navView.isInitialized) navView.setCheckedItem(checkedItemId)
    }

    private fun openPage(target: Class<*>) {
        if (javaClass == target) return
        val intent = Intent(this, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}