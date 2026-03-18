package com.example.rnschat

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL 
            setPadding(30, 30, 30, 30)
        }
        
        val tvStatus = TextView(this).apply { text = "RNS Chat Initializing..." }
        val etDest = EditText(this).apply { hint = "Destination Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send" }

        layout.addView(tvStatus); layout.addView(etDest); layout.addView(etMsg); layout.addView(btnSend)
        setContentView(layout)

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        val py = Python.getInstance()
        val rnsManager = py.getModule("rns_manager").get("ChatManager")?.call(object {
            fun onMessage(msg: String) {
                runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
            }
        })

        btnSend.setOnClickListener {
            rnsManager?.callAttr("send_text", etDest.text.toString(), etMsg.text.toString())
        }
    }
}