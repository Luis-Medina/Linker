package com.luismedinaweb.linker

import android.util.Log

import com.google.gson.Gson
import com.google.gson.stream.JsonReader

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.StringReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable


/**
 * Created by Luis on 4/11/2015.
 */
class PortSniffer(private val ipAddress: String) : Callable<String> {
    private val gson = Gson()

    @Throws(Exception::class)
    override fun call(): String? {

        var socket: Socket? = null
        var out: OutputStream? = null
        var `in`: BufferedInputStream? = null
        var port = -1
        var hostName = ipAddress

        for (i in portStart..portEnd) {
            var reader: JsonReader? = null
            if (Thread.currentThread().isInterrupted) {
                break
            }
            try {
                socket = Socket()
                val address = InetSocketAddress(ipAddress, i)
                socket.connect(address, 3000)
                out = socket.getOutputStream()
                `in` = BufferedInputStream(socket.getInputStream())

                val fromServer: String
                var fromUser: NetworkPacket

                fromUser = NetworkPacket(NetworkPacket.TYPE.CLIENT_HELLO, "Hello")
                out!!.write(gson.toJson(fromUser).toByteArray(charset("UTF-8")))

                val buffer = ByteArray(1024)
                if (`in`.read(buffer) > 0) {
                    fromServer = String(buffer, Charset.forName("UTF-8"))
                    reader = JsonReader(StringReader(fromServer))
                    reader.isLenient = true
                    val packetReceived = gson.fromJson<NetworkPacket>(reader, NetworkPacket::class.java)
                    if (packetReceived.getType() === NetworkPacket.TYPE.SERVER_HELLO) {
                        port = i
                        try {
                            val inetAddress = InetAddress.getByName(ipAddress)
                            hostName = inetAddress.canonicalHostName
                        } catch (e: Exception) {
                        }

                    }
                    fromUser = NetworkPacket(NetworkPacket.TYPE.TERMINATE, "OK")
                    out.write(gson.toJson(fromUser).toByteArray(charset("UTF-8")))
                }
            } catch (ex: Exception) {
                Log.e(LOG_TAG, ex.message)
            } finally {
                try {
                    out!!.close()
                } catch (e: Exception) {
                }

                try {
                    `in`!!.close()
                } catch (e: Exception) {
                }

                try {
                    socket!!.close()
                } catch (e: Exception) {
                }

                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "" + e.message)
                    }

                }
            }
            if (port > 0) {
                break
            }
        }

        return if (port > 0) {
            "$ipAddress;$port;$hostName"
        } else {
            null
        }


    }

    companion object {

        private val LOG_TAG = PortSniffer::class.java.simpleName
        private val portStart = 51111
        private val portEnd = 51131
    }

}
