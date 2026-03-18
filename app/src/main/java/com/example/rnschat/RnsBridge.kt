package com.example.rnschat
import com.chaquo.python.Python

object RNSBridge {
    private val py = Python.getInstance()
    private val worker = py.getModule("rns_worker")

    fun start(bt: BluetoothService) = worker.callAttr("start", py.getModule("bt_wrapper").callAttr("BtWrapper", bt)).toString()
    fun sendMessage(dest: String, txt: String) = worker.callAttr("send_message", dest, txt).toString()
    fun sendImage(dest: String, base64: String) = worker.callAttr("send_image", dest, base64).toString()
}