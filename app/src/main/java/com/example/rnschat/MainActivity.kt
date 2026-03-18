package com.example.rnschat

import android.annotation.SuppressLint
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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val tvTitle = TextView(this).apply { text = "Select Paired RNode:" }
        val spinner = Spinner(this)
        
        // Load Paired Devices
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val devices = btManager.adapter.bondedDevices
        val deviceNames = devices.map { "${it.name} (${it.address})" }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)

        val btnConnect = Button(this).apply { text = "Connect RNode" }
        val etDest = EditText(this).apply { hint = "Recipient Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send"; isEnabled = false }
        val tvLog = TextView(this).apply { text = "Log:\n" }

        layout.addView(tvTitle); layout.addView(spinner); layout.addView(btnConnect)
        layout.addView(etDest); layout.addView(etMsg); layout.addView(btnSend); layout.addView(tvLog)
        setContentView(layout)

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        btnConnect.setOnClickListener {
            val selected = spinner.selectedItem?.toString() ?: return@setOnClickListener
            val mac = selected.substringAfter("(").substringBefore(")")
            
            val py = Python.getInstance()
            rnsManager = py.getModule("rns_manager").get("ChatManager")?.call(object {
                fun onMessage(msg: String) { runOnUiThread { tvLog.append("RECV: $msg\n") } }
                fun onLog(log: String) { runOnUiThread { tvLog.append("$log\n") } }
            }, mac)
            
            btnConnect.isEnabled = false
            btnSend.isEnabled = true
        }

        btnSend.setOnClickListener {
            rnsManager?.callAttr("send_text", etDest.text.toString(), etMsg.text.toString())
            tvLog.append("SENT: ${etMsg.text}\n")
            etMsg.text.clear()
        }
    }
}