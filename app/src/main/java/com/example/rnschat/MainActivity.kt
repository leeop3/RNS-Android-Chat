package com.example.rnschat
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,40,40,40) }
        val etMac = EditText(this).apply { hint = "RNode BT MAC" }
        val btnStart = Button(this).apply { text = "Start RNS" }
        val etDest = EditText(this).apply { hint = "Recipient Hash" }
        val etMsg = EditText(this).apply { hint = "Message" }
        val btnSend = Button(this).apply { text = "Send" }
        val tvLog = TextView(this).apply { text = "Status: Idle" }

        layout.addView(etMac); layout.addView(btnStart); layout.addView(etDest); layout.addView(etMsg); layout.addView(btnSend); layout.addView(tvLog)
        setContentView(layout)

        btnStart.setOnClickListener {
            val service = RnsService()
            lifecycleScope.launch {
                val connected = withContext(Dispatchers.IO) { service.btService.connect(etMac.text.toString()) }
                if (connected) tvLog.text = "RNS Address: " + withContext(Dispatchers.IO) { RNSBridge.start(service.btService) }
            }
        }
        btnSend.setOnClickListener { RNSBridge.sendMessage(etDest.text.toString(), etMsg.text.toString()) }
    }
}