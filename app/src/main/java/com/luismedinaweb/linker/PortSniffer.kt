package com.luismedinaweb.linker

import android.net.ConnectivityManager
import com.luismedinaweb.LinkerPacket
import com.luismedinaweb.LinkerProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
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
            try {
                socket.use {
                    val address = InetSocketAddress(ipAddress, i)
                    it.connect(address, 150)

                    // Say hello to server
                    it.getOutputStream().sayHello()

                    val packetReceived = gson.fromJson(it.getInputStream().bufferedReader(), LinkerPacket::class.java)
                    if (packetReceived.type == LinkerPacket.SERVERHELLO) {
                        return ServerData(ipAddress, i)
                    }
                }
            } catch (ex: Exception) {
                if (ex is ConnectException) {
                    break@loop
                }
            }
        }
        return null
    }

    companion object {
        private val LOG_TAG = PortSniffer::class.java.simpleName
        private val portStart = LinkerProtocol.portStart
        private val portEnd = LinkerProtocol.portEnd
    }

}
