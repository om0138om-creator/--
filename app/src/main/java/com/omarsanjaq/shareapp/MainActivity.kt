package com.omarsanjaq.shareapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var sendCard: CardView
    private lateinit var receiveCard: CardView
    private lateinit var aboutButton: MaterialButton

    private val PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        checkPermissions()
    }

    private fun initViews() {
        sendCard = findViewById(R.id.sendCard)
        receiveCard = findViewById(R.id.receiveCard)
        aboutButton = findViewById(R.id.aboutButton)
    }

    private fun setupListeners() {
        sendCard.setOnClickListener {
            if (hasRequiredPermissions()) {
                startActivity(Intent(this, SendActivity::class.java))
            } else {
                requestPermissions()
            }
        }

        receiveCard.setOnClickListener {
            if (hasRequiredPermissions()) {
                startActivity(Intent(this, ReceiveActivity::class.java))
            } else {
                requestPermissions()
            }
        }

        aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        } else {
            permissions.addAll(listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.addAll(listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ))
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        permissions.addAll(listOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        ))

        return permissions.toTypedArray()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            getRequiredPermissions(),
            PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "تم منح جميع الأذونات", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "بعض الأذونات مطلوبة لعمل التطبيق", Toast.LENGTH_LONG).show()
            }
        }
    }
}
