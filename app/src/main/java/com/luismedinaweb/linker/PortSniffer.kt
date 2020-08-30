package com.luismedinaweb.linker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


/**
 * Created by Luis on 4/11/2015.
 */
class PortSniffer(private val ipAddress: String) {

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

                socket.getInputStream().bufferedReader().use {
                    val packetReceived = gson.fromJson(it, NetworkPacket::class.java)
                    if (packetReceived.getType() == NetworkPacket.TYPE.SERVER_HELLO) {
                        val hostName = try {
                            val inetAddress = InetAddress.getByName(ipAddress)
                            inetAddress.canonicalHostName
                        } catch (e: Exception) {
                            ipAddress
                        }
                        return ServerData(ipAddress, i, hostName)
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
