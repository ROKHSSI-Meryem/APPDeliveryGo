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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etCity: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etCity = findViewById(R.id.etCity)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.registerProgressBar)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        setupPasswordVisibility()

        btnRegister.setOnClickListener {
            registerUser()
        }

        // Inscription depuis le clavier (bouton "Done")
        etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registerUser()
                true
            } else {
                false
            }
        }

        // LoginActivity est déjà dans la pile : on revient simplement dessus
        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupPasswordVisibility() {

        val togglePassword =
            findViewById<TextView>(R.id.toggleRegisterPassword)

        val toggleConfirmPassword =
            findViewById<TextView>(R.id.toggleConfirmPassword)

        togglePassword.setOnClickListener {
            togglePasswordVisibility(etPassword, togglePassword)
        }

        toggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, toggleConfirmPassword)
        }
    }

    private fun togglePasswordVisibility(
        editText: EditText,
        toggleView: TextView
    ) {
        val isHidden =
            editText.transformationMethod is PasswordTransformationMethod

        if (isHidden) {
            editText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            toggleView.text = "🙈"
        } else {
            editText.transformationMethod =
                PasswordTransformationMethod.getInstance()
            toggleView.text = "👁"
        }

        editText.setSelection(editText.text.length)
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        tvBackToLogin.isEnabled = !loading
    }

    private fun registerUser() {

        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val city = etCity.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (firstName.isEmpty()) {
            etFirstName.error = getString(R.string.error_first_name)
            etFirstName.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            etLastName.error = getString(R.string.error_last_name)
            etLastName.requestFocus()
            return
        }

        if (city.isEmpty()) {
            etCity.error = getString(R.string.error_city)
            etCity.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.error_invalid_email)
            etEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = getString(R.string.error_password_short)
            etPassword.requestFocus()
            return
        }

        if (confirmPassword != password) {
            etConfirmPassword.error = getString(R.string.error_password_mismatch)
            etConfirmPassword.requestFocus()
            return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    setLoading(false)

                    Toast.makeText(
                        this,
                        getRegisterErrorMessage(task.exception),
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                val user = auth.currentUser

                if (user == null) {
                    setLoading(false)
                    openHome()
                    return@addOnCompleteListener
                }

                // Informations sauvegardées par compte (uid) : un autre compte
                // sur le même téléphone ne verra pas ce profil
                getSharedPreferences("driver_data_${user.uid}", MODE_PRIVATE)
                    .edit()
                    .putString("firstName", firstName)
                    .putString("lastName", lastName)
                    .putString("city", city)
                    .putString("email", email)
                    .apply()

                val profileUpdates = userProfileChangeRequest {
                    displayName = "$firstName $lastName"
                }

                // Que la mise à jour du nom réussisse ou non, le compte est créé
                user.updateProfile(profileUpdates)
                    .addOnCompleteListener {

                        setLoading(false)

                        Toast.makeText(
                            this,
                            getString(R.string.register_success),
                            Toast.LENGTH_SHORT
                        ).show()

                        openHome()
                    }
            }
    }

    private fun getRegisterErrorMessage(exception: Exception?): String {
        return when (exception) {
            // Doit être avant InvalidCredentials (WeakPassword en hérite)
            is FirebaseAuthWeakPasswordException ->
                getString(R.string.error_weak_password)

            is FirebaseAuthUserCollisionException ->
                getString(R.string.error_email_in_use)

            is FirebaseAuthInvalidCredentialsException ->
                getString(R.string.error_invalid_email)

            is FirebaseNetworkException ->
                getString(R.string.error_network)

            else -> getString(R.string.error_register_failed)
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