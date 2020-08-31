package com.luismedinaweb.linker

import android.net.ConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket


/**
 * Created by Luis on 4/11/2015.
 */
class PortSniffer(private val ipAddress: String, private val connectivityManager: ConnectivityManager?) {

    @Throws(Exception::class)
    fun call(scope: CoroutineScope): ServerData? {
        loop@ for (i in portStart..portEnd) {
            scope.ensureActive()
            val socket = Socket()
            var out: OutputStream? = null
            try {
                val address = InetSocketAddress(ipAddress, i)
                socket.connect(address, 150)
                out = socket.getOutputStream()

                // Say hello to server
                out.sayHello()

                socket.getInputStream().bufferedReader().use { reader ->
                    val packetReceived = gson.fromJson(reader, NetworkPacket::class.java)
                    if (packetReceived.getType() == NetworkPacket.TYPE.SERVER_HELLO) {
                        return ServerData(ipAddress, i)
                    }
                }
            } catch (ex: Exception) {
                if (ex is ConnectException) {
                    break@loop
                }
            } finally {
                cleanup(socket, out)
            }
        }
        return null
    }

    companion object {
        private val LOG_TAG = PortSniffer::class.java.simpleName
        private val portStart = 51111
        private val portEnd = 51151
    }

}
