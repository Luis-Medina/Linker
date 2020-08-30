package com.luismedinaweb.linker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket


class LinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val msgText = findViewById<View>(R.id.txt_message) as TextView
        val setupButton = findViewById<View>(R.id.btn_setup) as Button

        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                PrefHelper.serverData?.let {
                    msgText.text = "Sending link to " + it.serverName
                    setupButton.visibility = Button.GONE
                    handleText(intent, it)
                } ?: run {
                    msgText.text = "Please setup a host computer"
                    setupButton.visibility = Button.VISIBLE
                    setupButton.setOnClickListener {
                        val mainActivity = Intent(applicationContext, MainActivity::class.java)
                        mainActivity.putExtra("url", intent.getStringExtra(Intent.EXTRA_TEXT))
                        startActivity(mainActivity)
                    }
                }
            }
        }

    }

    private fun handleText(intent: Intent, serverData: ServerData) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            CoroutineScope(Dispatchers.IO).launch {
                sendLinkToServer(sharedText, serverData.ipAddress, serverData.port)
            }
        }
    }

    private suspend fun sendLinkToServer(url: String, ipAddress: String, serverPort: Int) {
        val result = doInBackground(url, ipAddress, serverPort)
        withContext(Dispatchers.Main) {
            processResult(result)
        }
    }

    private fun processResult(result: String?) {
        if (result != null) {
            Toast.makeText(this, result, Toast.LENGTH_LONG).show()
        }
        this.finish()
    }

    companion object {

        private val TAG = "LinkActivity"
    }

    private fun doInBackground(url: String, ipAddress: String, serverPort: Int): String {
        val socket = Socket()
        var out: OutputStream? = null
        var result = ""
        try {
            val address = InetSocketAddress(ipAddress, serverPort)
            socket.connect(address, 5000)
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
            Log.e(TAG, e.toString())
            result = e.toString()
        } finally {
            cleanup(socket, out)
        }
        return result
    }

}
