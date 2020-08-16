package com.luismedinaweb.linker

import android.util.Log

import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Callable

/**
 * Created by Luis on 4/11/2015.
 */
class PingNode(private val node: String) : Runnable {

    override fun run() {
        try {
            InetAddress.getByName(node).isReachable(150)
            //Log.d("PINGING: ", node);
        } catch (e: IOException) {
        }

    }
}