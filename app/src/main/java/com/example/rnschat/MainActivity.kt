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
            setPadding(30, 30, 30, 30)
        }
        spinner = Spinner(this)
        val btnConnect = Button(this).apply { text = "Connect RNode" }
        val etDest = EditText(this).apply { hint = "Recipient Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send" }
        tvLog = TextView(this).apply { text = "Log:\n" }

        layout.addView(spinner); layout.addView(btnConnect); layout.addView(etDest); layout.addView(etMsg); layout.addView(btnSend); layout.addView(tvLog)
        setContentView(layout)

        checkPermissions()
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        btnConnect.setOnClickListener {
            val selected = spinner.selectedItem?.toString() ?: return@setOnClickListener
            val mac = selected.substringAfter("(").substringBefore(")")
            startBridgeAndRNS(mac)
            btnConnect.isEnabled = false
        }

        btnSend.setOnClickListener {
            rnsManager?.callAttr("send_text", etDest.text.toString(), etMsg.text.toString())
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBridgeAndRNS(mac: String) {
        thread {
            try {
                // 1. Connect Bluetooth
                val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val device = btManager.adapter.getRemoteDevice(mac)
                val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                btSocket.connect()
                runOnUiThread { tvLog.append("BT Connected to RNode\n") }

                // 2. Start Local TCP Server
                val serverSocket = ServerSocket(4242)
                
                // 3. Start RNS in Python (it will connect to 127.0.0.1:4242)
                runOnUiThread {
                    val py = Python.getInstance()
                    rnsManager = py.getModule("rns_manager").get("ChatManager")?.call(object {
                        fun onMessage(msg: String) { runOnUiThread { tvLog.append("RECV: $msg\n") } }
                        fun onLog(log: String) { runOnUiThread { tvLog.append("$log\n") } }
                    })
                }

                val tcpSocket = serverSocket.accept()
                runOnUiThread { tvLog.append("Bridge Active: RNode <-> RNS\n") }

                // 4. Start Bi-directional Data Transfer
                val btIn = btSocket.inputStream
                val btOut = btSocket.outputStream
                val tcpIn = tcpSocket.getInputStream()
                val tcpOut = tcpSocket.getOutputStream()

                thread { copyStream(btIn, tcpOut) } // RNode to RNS
                thread { copyStream(tcpIn, btOut) } // RNS to RNode

            } catch (e: Exception) {
                runOnUiThread { tvLog.append("Bridge Error: ${e.message}\n") }
            }
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        while (true) {
            try {
                val bytes = input.read(buffer)
                if (bytes <= 0) break
                output.write(buffer, 0, bytes)
                output.flush()
            } catch (e: Exception) { break }
        }
    }

    // ... (Keep your checkPermissions and loadPairedDevices functions from before)
    private fun checkPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (perms.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, 101)
        } else { loadPairedDevices() }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val devices = btManager.adapter.bondedDevices
        val deviceNames = devices.map { "${it.name} (${it.address})" }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
    }
}