package com.example.rnschat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    private var rnsManager: PyObject? = null
    private lateinit var spinner: Spinner
    private lateinit var btnConnect: Button
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val tvTitle = TextView(this).apply { text = "Select Paired RNode:" }
        spinner = Spinner(this)
        btnConnect = Button(this).apply { text = "Connect RNode" }
        val etDest = EditText(this).apply { hint = "Recipient Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send"; isEnabled = false }
        tvLog = TextView(this).apply { text = "Log:\n" }

        layout.addView(tvTitle); layout.addView(spinner); layout.addView(btnConnect)
        layout.addView(etDest); layout.addView(etMsg); layout.addView(btnSend); layout.addView(tvLog)
        setContentView(layout)

        // Request permissions if needed
        checkBtPermissions()

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        btnConnect.setOnClickListener {
            val selected = spinner.selectedItem?.toString() ?: return@setOnClickListener
            val mac = selected.substringAfter("(").substringBefore(")")
            
            try {
                val py = Python.getInstance()
                val rnsModule = py.getModule("rns_manager")
                rnsManager = rnsModule.get("ChatManager")?.call(object {
                    @Suppress("unused")
                    fun onMessage(msg: String) { runOnUiThread { tvLog.append("RECV: $msg\n") } }
                    @Suppress("unused")
                    fun onLog(log: String) { runOnUiThread { tvLog.append("$log\n") } }
                }, mac)
                
                btnConnect.isEnabled = false
                btnSend.isEnabled = true
                tvLog.append("Connecting to $mac...\n")
            } catch (e: Exception) {
                tvLog.append("Error: ${e.message}\n")
            }
        }

        btnSend.setOnClickListener {
            val dest = etDest.text.toString()
            val msg = etMsg.text.toString()
            if (dest.isNotEmpty() && msg.isNotEmpty()) {
                rnsManager?.callAttr("send_text", dest, msg)
                tvLog.append("SENT: $msg\n")
                etMsg.text.clear()
            }
        }
    }

    private fun checkBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 101)
                return
            }
        }
        loadPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        try {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val devices = btManager.adapter.bondedDevices
            val deviceNames = devices.map { "${it.name} (${it.address})" }
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading devices: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPairedDevices()
        }
    }
}