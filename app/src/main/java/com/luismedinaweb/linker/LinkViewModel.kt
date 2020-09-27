package com.luismedinaweb.linker

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.luismedinaweb.LinkerPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        var result = ""
        try {
            socket.use {
                val address = InetSocketAddress(ipAddress, serverPort)
                it.connect(address, 1000)

                // Send link to server
                val linkPacket = LinkerPacket(LinkerPacket.LINK, url)
                it.getOutputStream().write(gson.toJson(linkPacket).toByteArray())

                // Read server response
                val response = gson.fromJson(it.getInputStream().bufferedReader(), LinkerPacket::class.java)
                result = if (response.type == LinkerPacket.ACK) {
                    "Link sent!"
                } else {
                    response.content ?: "Error"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
            result = e.toString()
        }
        return result
    }

    data class LinkResult(val message: String)
}