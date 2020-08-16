package com.luismedinaweb.linker

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class PrefHelper {

    private var serverPort: Int = defaultPort
    private var serverName: String = defaultServer
    private var ipAddress: String = defaultIP

    fun haveValidServer(): Boolean {
        return !(serverName == defaultServer || serverPort == defaultPort)
    }

    fun getServerPort(): Int {
        return serverPort
    }

    fun setServerPort(serverPort: Int) {
        sharedPreferences.edit().putInt("port", serverPort).apply()
        this.serverPort = serverPort
    }

    fun getServerName(): String? {
        return serverName
    }

    fun setServerName(serverName: String) {
        sharedPreferences.edit().putString("server", serverName.toUpperCase()).apply()
        this.serverName = serverName.toUpperCase()
    }

    fun getIpAddress(): String {
        return ipAddress
    }

    fun setIpAddress(ipAddress: String) {
        sharedPreferences.edit().putString("ipAddress", ipAddress).apply()
        this.ipAddress = ipAddress
    }

    companion object {
        @Volatile
        private var INSTANCE: PrefHelper? = null

        val defaultServer = "N/A"
        val defaultIP = "N/A"
        val defaultPort = -1
        private lateinit var sharedPreferences: SharedPreferences

        fun getInstance(context: Context): PrefHelper {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = PrefHelper()
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                INSTANCE = instance
                return instance
            }
        }
    }
}