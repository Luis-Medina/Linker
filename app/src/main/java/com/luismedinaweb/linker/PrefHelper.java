package com.luismedinaweb.linker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Luis on 4/11/2015.
 */
public class PrefHelper {

    public static final String defaultServer = "N/A";
    public static final String defaultIP = "N/A";
    public static final int defaultPort = -1;
    private static PrefHelper prefHelper;
    private SharedPreferences sharedPreferences;
    private int serverPort;
    private String serverName;
    private String ipAddress;

    private PrefHelper(Context context){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        serverName = sharedPreferences.getString("server", defaultServer);
        serverPort = sharedPreferences.getInt("port", defaultPort);
        ipAddress = sharedPreferences.getString("ipAddress", defaultIP);
    }

    public static PrefHelper getInstance(Context context){
        if(prefHelper == null){
            prefHelper =  new PrefHelper(context);
        }
        return prefHelper;
    }

    public boolean haveValidServer(){
        if(serverName.equals(defaultServer) || serverPort == defaultPort){
            return false;
        }
        return true;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        sharedPreferences.edit().putInt("port", serverPort).commit();
        this.serverPort = serverPort;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        sharedPreferences.edit().putString("server", serverName.toUpperCase()).commit();
        this.serverName = serverName.toUpperCase();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        sharedPreferences.edit().putString("ipAddress", ipAddress).commit();
        this.ipAddress = ipAddress;
    }
}
