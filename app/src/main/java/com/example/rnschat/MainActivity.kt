package com.example.rnschat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var tvLog: TextView

    // Modern way to handle permission requests
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true) {
            loadPairedDevices()
        } else {
            tvLog.text = "Permission Denied: Cannot see Bluetooth devices."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- UI Setup ---
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40) 
        }
        
        spinner = Spinner(this)
        val btnStart = Button(this).apply { text = "Start RNS" }
        val etDest = EditText(this).apply { hint = "Recipient Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send" }
        tvLog = TextView(this).apply { text = "Status: Initializing..." }

        layout.addView(TextView(this).apply { text = "Select RNode:" })
        layout.addView(spinner)
        layout.addView(btnStart)
        layout.addView(etDest)
        layout.addView(etMsg)
        layout.addView(btnSend)
        layout.addView(tvLog)
        setContentView(layout)

        // Trigger permission check
        checkAndRequestPermissions()

        btnStart.setOnClickListener {
            val selected = spinner.selectedItem?.toString() ?: ""
            if (!selected.contains("(")) {
                Toast.makeText(this, "Select a device first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mac = selected.substringAfter("(").substringBefore(")")
            val service = RnsService()
            
            lifecycleScope.launch {
                tvLog.text = "Connecting to $mac..."
                val connected = withContext(Dispatchers.IO) { service.btService.connect(mac) }
                if (connected) {
                    val addr = withContext(Dispatchers.IO) { RNSBridge.start(service.btService) }
                    tvLog.text = "RNS Online: $addr"
                } else {
                    tvLog.text = "Failed to connect to RNode via Bluetooth."
                }
            }
        }

        btnSend.setOnClickListener {
            val res = RNSBridge.sendMessage(etDest.text.toString(), etMsg.text.toString())
            Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missing.isEmpty()) {
            loadPairedDevices()
        } else {
            requestPermissionLauncher.launch(missing)
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val devices = btManager.adapter.bondedDevices
        val names = devices.map { "${it.name} (${it.address})" }
        
        if (names.isEmpty()) {
            tvLog.text = "No paired RNodes found. Pair in Android Settings first."
        } else {
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            tvLog.text = "Ready to connect."
        }
    }
}