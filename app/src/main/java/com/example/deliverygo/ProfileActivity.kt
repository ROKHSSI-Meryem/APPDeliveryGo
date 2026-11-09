package com.example.deliverygo

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupNavigation(R.id.nav_profile)

        val user = FirebaseAuth.getInstance().currentUser
        val prefs = user?.let { getSharedPreferences("driver_data_${it.uid}", MODE_PRIVATE) }

        val firstName = prefs?.getString("firstName", null) ?: "-"
        val lastName = prefs?.getString("lastName", null) ?: "-"
        val city = prefs?.getString("city", null) ?: "-"
        val email = user?.email ?: getString(R.string.profile_not_logged)

        findViewById<TextView>(R.id.profileFirstName).text = firstName
        findViewById<TextView>(R.id.profileLastName).text = lastName
        findViewById<TextView>(R.id.profileCity).text = city
        findViewById<TextView>(R.id.profileEmail).text = email

        val qrContent = "firstName=$firstName\nlastName=$lastName\ncity=$city\nemail=$email"
        findViewById<ImageView>(R.id.qrImage).setImageBitmap(generateQr(qrContent))
    }

    private fun generateQr(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
}