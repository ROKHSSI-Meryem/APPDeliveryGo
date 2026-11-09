package com.example.deliverygo

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)

        setupPasswordVisibility()

        btnLogin.setOnClickListener {
            loginUser()
        }

        // Login depuis le clavier (bouton "Done")
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginUser()
                true
            } else {
                false
            }
        }

        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun setupPasswordVisibility() {

        val togglePassword =
            findViewById<TextView>(R.id.toggleLoginPassword)

        togglePassword.setOnClickListener {

            val isHidden =
                etPassword.transformationMethod is PasswordTransformationMethod

            if (isHidden) {
                etPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                togglePassword.text = "🙈"
            } else {
                etPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                togglePassword.text = "👁"
            }

            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        tvForgotPassword.isEnabled = !loading
        tvCreateAccount.isEnabled = !loading
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.error_invalid_email)
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.error_empty_password)
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                setLoading(false)

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        getString(R.string.login_success),
                        Toast.LENGTH_SHORT
                    ).show()

                    openHome()

                } else {

                    Toast.makeText(
                        this,
                        getLoginErrorMessage(task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun getLoginErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException ->
                getString(R.string.error_wrong_credentials)

            is FirebaseNetworkException ->
                getString(R.string.error_network)

            else -> getString(R.string.error_login_failed)
        }
    }

    private fun resetPassword() {

        val email = etEmail.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.error_enter_email)
            etEmail.requestFocus()
            return
        }

        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->

                setLoading(false)

                if (task.isSuccessful) {
                    // Message générique : ne révèle pas si le compte existe
                    Toast.makeText(
                        this,
                        getString(R.string.reset_email_sent),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val message =
                        if (task.exception is FirebaseNetworkException) {
                            getString(R.string.error_network)
                        } else {
                            getString(R.string.error_reset_failed)
                        }

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun openHome() {

        val intent = Intent(this, MainActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}