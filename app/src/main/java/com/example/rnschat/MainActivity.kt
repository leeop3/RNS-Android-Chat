package com.example.rnschat

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    
    // We declare this at the class level so it's accessible everywhere
    private var rnsManager: PyObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- UI SETUP ---
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL 
            setPadding(30, 30, 30, 30)
        }
        
        val tvStatus = TextView(this).apply { text = "RNS Chat Initializing..." }
        val etDest = EditText(this).apply { hint = "Recipient Hash (Hex)" }
        val etMsg = EditText(this).apply { hint = "Type Message..." }
        val btnSend = Button(this).apply { text = "Send Message" }

        layout.addView(tvStatus)
        layout.addView(etDest)
        layout.addView(etMsg)
        layout.addView(btnSend)
        setContentView(layout)

        // --- PYTHON / RNS SETUP ---
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        
        val py = Python.getInstance()
        val rnsModule = py.getModule("rns_manager")
        
        // Initialize the ChatManager class from our Python script
        rnsManager = rnsModule.get("ChatManager")?.call(object {
            @Suppress("unused") // Used by Python callback
            fun onMessage(msg: String) {
                runOnUiThread { 
                    Toast.makeText(this@MainActivity, "Received: $msg", Toast.LENGTH_LONG).show() 
                }
            }
        })

        tvStatus.text = "RNS Online"

        // --- BUTTON LOGIC ---
        btnSend.setOnClickListener {
            val destination = etDest.text.toString()
            val message = etMsg.text.toString()
            
            if (destination.isNotEmpty() && message.isNotEmpty()) {
                try {
                    rnsManager?.callAttr("send_text", destination, message)
                    etMsg.text.clear()
                    Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Enter Destination and Message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}