package com.example.rnschat
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.util.*

class BluetoothService(private val context: Context) {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        return try {
            val device = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.getRemoteDevice(address)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
            true
        } catch (e: Exception) { false }
    }
    fun disconnect() = socket?.close()
    fun readBytes(n: Int): ByteArray {
        val avail = socket?.inputStream?.available() ?: 0
        if (avail == 0) return byteArrayOf()
        val res = ByteArray(if (n == -1 || n > avail) avail else n)
        socket?.inputStream?.read(res)
        return res
    }
    fun writeBytes(b: ByteArray) { socket?.outputStream?.write(b); socket?.outputStream?.flush() }
}