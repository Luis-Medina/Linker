package com.luismedinaweb.linker

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class LinkViewModel : ViewModel() {

    private val _result: MutableLiveData<LinkResult> = MutableLiveData()
    val result: LiveData<LinkResult> = _result

    companion object {
        private const val TAG = "LinkViewModel"
    }

    fun sendLinkToServer(url: String, ipAddress: String, serverPort: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            // Blah, let's let the user know what's happening for a sec
            delay(1000)
            val result = connectToServer(url, ipAddress, serverPort)
            _result.postValue(LinkResult(result))
        }
    }

    private fun connectToServer(url: String, ipAddress: String, serverPort: Int): String {
        val socket = Socket()
        var out: OutputStream? = null
        var result = ""
        try {
            val address = InetSocketAddress(ipAddress, serverPort)
            socket.connect(address, 1000)
            out = socket.getOutputStream()

            // Send link to server
            val linkPacket = NetworkPacket(NetworkPacket.TYPE.LINK, url)
            out.write(gson.toJson(linkPacket).toByteArray())

            // Read server response
            result = socket.getInputStream().bufferedReader().use {
                val response = gson.fromJson(it, NetworkPacket::class.java)
                if (response.getType() == NetworkPacket.TYPE.ACK) {
                    "Link sent!"
                } else {
                    response.content ?: "Error"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
            result = e.toString()
        } finally {
            cleanup(socket, out)
        }
        return result
    }

    data class LinkResult(val message: String)
}