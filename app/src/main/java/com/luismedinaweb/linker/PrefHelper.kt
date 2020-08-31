package com.luismedinaweb.linker

import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object PrefHelper {

    private lateinit var sharedPreferences: SharedPreferences
    private const val IP_KEY: String = "ipAddress"
    private const val PORT_KEY: String = "port"

    var serverData: ServerData? = null
        set(value) {
            if (value != null) {
                sharedPreferences.edit().putInt(PORT_KEY, value.port).apply()
                sharedPreferences.edit().putString(IP_KEY, value.ipAddress).apply()
            } else {
                sharedPreferences.edit().clear().apply()
            }
            field = value
        }

    fun initialize(application: MainApplication) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        val port = sharedPreferences.getInt(PORT_KEY, 0)
        val ipAddress = sharedPreferences.getString(IP_KEY, null)
        if (port >= 0 && ipAddress != null) {
            serverData = ServerData(ipAddress, port)
        }
    }

    fun hasValidServer(): Boolean {
        return serverData != null
    }
}