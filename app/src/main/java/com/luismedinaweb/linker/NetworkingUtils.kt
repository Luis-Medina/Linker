package com.luismedinaweb.linker

import com.google.gson.Gson
import java.io.OutputStream
import java.net.Socket

val gson = Gson()

fun OutputStream.sayHello() {
    val helloPacket = NetworkPacket(NetworkPacket.TYPE.CLIENT_HELLO, "Hello")
    this.write(gson.toJson(helloPacket).toByteArray())
}

fun OutputStream.sayGoodbye() {
    val goodbyePacket = NetworkPacket(NetworkPacket.TYPE.TERMINATE, "OK")
    this.write(gson.toJson(goodbyePacket).toByteArray())
}

fun cleanup(socket: Socket, outputStream: OutputStream?) {
    try {
        outputStream?.sayGoodbye()
        outputStream?.close()
    } catch (e: Exception) {
    }
    try {
        socket.close()
    } catch (e: Exception) {
    }
}