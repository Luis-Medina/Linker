package com.luismedinaweb.linker

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.io.StringReader
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset


class LinkActivity : AppCompatActivity() {

    private var prefHelper: PrefHelper? = null
    private var fm: FragmentManager? = null
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        prefHelper = PrefHelper.getInstance(this)
        val msgText = findViewById<View>(R.id.txt_message) as TextView
        val setupButton = findViewById<View>(R.id.btn_setup) as Button

        fm = supportFragmentManager

        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                if (prefHelper!!.haveValidServer()) {
                    msgText.text = "Sending link to " + prefHelper?.getServerName()
                    setupButton.visibility = Button.GONE
                    handleText(intent)
                } else {
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

    private fun handleText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            val prefHelper = PrefHelper.getInstance(this@LinkActivity)
            CoroutineScope(Dispatchers.IO).launch {
                sendLinkToServer(sharedText, prefHelper.getIpAddress(), prefHelper.getServerPort())
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
        private val SEND_TASK_FRAGMENT = "send_task_fragment"
    }

    fun doInBackground(url: String, ipAddress: String, serverPort: Int): String {
        var result: String = ""
        var out: OutputStream? = null
        var socket: Socket? = null
        var `in`: BufferedInputStream? = null
        var reader: JsonReader? = null
        try {
            socket = Socket()
            val address = InetSocketAddress(ipAddress, serverPort)
            socket.connect(address, 5000)
            out = socket.getOutputStream()
            `in` = BufferedInputStream(socket.getInputStream())

            val fromServer: String
            var fromUser: NetworkPacket

            fromUser = NetworkPacket(NetworkPacket.TYPE.LINK, url)
            out!!.write(gson.toJson(fromUser).toByteArray(charset("UTF-8")))

            val buffer = ByteArray(1024)
            if (`in`.read(buffer) > 0) {
                fromServer = String(buffer, Charset.forName("UTF-8"))
                reader = JsonReader(StringReader(fromServer))
                reader.isLenient = true
                val packetReceived = gson.fromJson<NetworkPacket>(reader, NetworkPacket::class.java)
                if (packetReceived.getType() === NetworkPacket.TYPE.ACK) {
                    result = "Link sent!"
                } else {
                    result = packetReceived.content ?: ""
                }
                fromUser = NetworkPacket(NetworkPacket.TYPE.TERMINATE, "OK")
                out.write(gson.toJson(fromUser).toByteArray(charset("UTF-8")))
            }
        } catch (e: Exception) {
            Log.e(LinkActivity.TAG, e.toString())
            result = e.toString()
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (e: Exception) {
                }

            }
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (e: Exception) {
                }

            }
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: Exception) {
                }

            }
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(LinkActivity.TAG, "" + e.message)
                }

            }
        }
        return result
    }

}
