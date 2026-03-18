package com.example.rnschat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
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
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private var rnsManager: PyObject? = null
    private lateinit var spinner: Spinner
    private lateinit var tvLog: TextView
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        tvLog = TextView(this).apply { text = "Status: Initializing...\n" }
        spinner = Spinner(this)
        
        val btnReload = Button(this).apply { text = "Refresh Paired Devices" }
        val btnConnect = Button(this).apply { text = "Connect RNode" }
        val etDest = EditText(this).apply { hint = "Recipient Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send Message" }

        layout.addView(tvLog)
        layout.addView(TextView(this).apply { text = "Select RNode:" })
        layout.addView(spinner)
        layout.addView(btnReload)
        layout.addView(btnConnect)
        layout.addView(etDest)
        layout.addView(etMsg)
        layout.addView(btnSend)

        setContentView(layout)

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        // Check permissions immediately
        checkPermissions()

        btnReload.setOnClickListener { loadPairedDevices() }

        btnConnect.setOnClickListener {
            val selected = spinner.selectedItem?.toString() ?: ""
            if (selected.contains("(")) {
                val mac = selected.substringAfter("(").substringBefore(")")
                startBridgeAndRNS(mac)
                btnConnect.isEnabled = false
            } else {
                Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show()
            }
        }

        btnSend.setOnClickListener {
            val dest = etDest.text.toString()
            val msg = etMsg.text.toString()
            rnsManager?.callAttr("send_text", dest, msg)
            tvLog.append("Me: $msg\n")
            etMsg.text.clear()
        }
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 101)
        } else {
            loadPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        try {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val devices = btManager.adapter.bondedDevices
            if (devices.isEmpty()) {
                tvLog.append("No paired devices found. Pair RNode in Android Settings first.\n")
            }
            val deviceNames = devices.map { "${it.name} (${it.address})" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
            spinner.adapter = adapter
            tvLog.append("Bluetooth list updated.\n")
        } catch (e: Exception) {
            tvLog.append("BT Error: ${e.message}\n")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBridgeAndRNS(mac: String) {
        thread {
            try {
                runOnUiThread { tvLog.append("Connecting to Bluetooth...\n") }
                val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val device = btManager.adapter.getRemoteDevice(mac)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()

                val serverSocket = ServerSocket(4242)
                
                runOnUiThread {
                    tvLog.append("BT Connected. Starting RNS...\n")
                    val py = Python.getInstance()
                    rnsManager = py.getModule("rns_manager").get("ChatManager")?.call(object {
                        fun onMessage(msg: String) { runOnUiThread { tvLog.append("Friend: $msg\n") } }
                        fun onLog(log: String) { runOnUiThread { tvLog.append("$log\n") } }
                    })
                }

                val tcpSocket = serverSocket.accept()
                runOnUiThread { tvLog.append("Bridge Active. You are online.\n") }

                val btIn = socket.inputStream
                val btOut = socket.outputStream
                val tcpIn = tcpSocket.getInputStream()
                val tcpOut = tcpSocket.getOutputStream()

                thread { copyStream(btIn, tcpOut) }
                thread { copyStream(tcpIn, btOut) }

            } catch (e: Exception) {
                runOnUiThread { tvLog.append("Error: ${e.message}\n") }
            }
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        try {
            while (true) {
                val bytes = input.read(buffer)
                if (bytes <= 0) break
                output.write(buffer, 0, bytes)
                output.flush()
            }
        } catch (e: Exception) { }
    }
}