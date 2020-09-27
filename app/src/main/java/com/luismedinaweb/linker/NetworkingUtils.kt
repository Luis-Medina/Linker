package com.luismedinaweb.linker

import com.google.gson.Gson
import com.luismedinaweb.LinkerPacket
import java.io.OutputStream

val gson = Gson()

fun OutputStream.sayHello() {
    val helloPacket = LinkerPacket(LinkerPacket.CLIENTHELLO, "Hello")
    this.write(gson.toJson(helloPacket).toByteArray())
}