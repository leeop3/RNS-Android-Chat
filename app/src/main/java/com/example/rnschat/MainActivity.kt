package com.example.rnschat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    private var rnsManager: PyObject? = null
    private lateinit var tvLog: TextView

    @SuppressLint("MissingPermission") // Permissions handled by pairing manually first
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        // --- BT Device Selection ---
        val tvDevice = TextView(this).apply { text = "1. Select Paired RNode:" }
        val spinner = Spinner(this)
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val pairedDevices = bluetoothManager.adapter.bondedDevices
        val deviceNames = pairedDevices.map { "${it.name} (${it.address})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        spinner.adapter = adapter

        // --- UI Elements ---
        val btnConnect = Button(this).apply { text = "Connect & Start RNS" }
        val etDest = EditText(this).apply { hint = "Recipient Hash (Hex)" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send" ; isEnabled = false }
        tvLog = TextView(this).apply { text = "Status: Idle\n" }

        layout.addView(tvDevice); layout.addView(spinner); layout.addView(btnConnect)
        layout.addView(etDest); layout.addView(etMsg); layout.addView(btnSend); layout.addView(tvLog)
        setContentView(layout)

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        btnConnect.setOnClickListener {
            val selected = spinner.selectedItem.toString()
            val mac = selected.substringAfter("(").substringBefore(")")
            
            val py = Python.getInstance()
            val rnsModule = py.getModule("rns_manager")
            
            rnsManager = rnsModule.get("ChatManager")?.call(object {
                fun onMessage(msg: String) {
                    runOnUiThread { tvLog.append("Recv: $msg\n") }
                }
                fun onLog(log: String) {
                    runOnUiThread { tvLog.append("$log\n") }
                }
            }, mac)
            
            btnConnect.isEnabled = false
            btnSend.isEnabled = true
        }

        btnSend.setOnClickListener {
            val dest = etDest.text.toString()
            val msg = etMsg.text.toString()
            if (dest.length == 20 || dest.length == 32) { // Basic RNS hash check
                rnsManager?.callAttr("send_text", dest, msg)
                tvLog.append("Sent: $msg\n")
            } else {
                Toast.makeText(this, "Invalid Destination Hash", Toast.LENGTH_SHORT).show()
            }
        }
    }
}