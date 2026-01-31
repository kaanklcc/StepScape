package com.example.stepscape

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.example.stepscape.data.health.HealthConnectManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        
        private val HEALTH_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }
    
    @Inject
    lateinit var healthConnectManager: HealthConnectManager

    private lateinit var healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")
        
        healthConnectPermissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            Log.d(TAG, "Permissions result: $granted")
            if (!granted.containsAll(HEALTH_PERMISSIONS)) {
                Toast.makeText(this, "Health Connect izni gerekli", Toast.LENGTH_SHORT).show()
            }
        }
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        checkHealthConnectAvailability()
    }
    
    private fun checkHealthConnectAvailability() {
        if (healthConnectManager.needsUpdate()) {
            showHealthConnectInstallDialog()
        }
    }

    private fun showHealthConnectInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle("Health Connect Gerekli")
            .setMessage("Adım verilerini okumak için Health Connect uygulamasını yüklemeniz gerekiyor.")
            .setPositiveButton("Yükle") { _, _ ->
                openPlayStoreForHealthConnect()
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun openPlayStoreForHealthConnect() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            }
            startActivity(intent)
        }
    }
    fun requestHealthConnectPermissions() {
        Log.d(TAG, "Requesting Health Connect permissions...")
        healthConnectPermissionLauncher.launch(HEALTH_PERMISSIONS)
    }
}