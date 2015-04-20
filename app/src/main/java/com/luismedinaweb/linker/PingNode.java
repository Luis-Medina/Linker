package com.luismedinaweb.linker;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Callable;

/**
 * Created by Luis on 4/11/2015.
 */
public class PingNode implements Runnable {

    private String node;

    public PingNode(String node) {
        this.node = node;
    }

    @Override
    public void run() {
        try {
            InetAddress.getByName(node).isReachable(150);
            //Log.d("PINGING: ", node);
        } catch (IOException e) {
        }

    }
}