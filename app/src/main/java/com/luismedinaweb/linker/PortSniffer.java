package com.luismedinaweb.linker;

import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

import linker.LinkerPacket;
import linker.Protocol;

/**
 * Created by Luis on 4/11/2015.
 */
public class PortSniffer implements Callable {

    private final String LOG_TAG = "PortSniffer";
    private String ipAddress;

    public PortSniffer(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String call() throws Exception {

        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        int port = -1;
        String hostName = ipAddress;

        for (int i = Protocol.portStart; i <= Protocol.portEnd; i++) {

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
                socket = new Socket();
                SocketAddress address = new InetSocketAddress(ipAddress, i);
                socket.connect(address, 3000);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                LinkerPacket fromServer;
                LinkerPacket fromUser;

                fromUser = new LinkerPacket(LinkerPacket.CLIENTHELLO, "Hello");
                out.writeObject(fromUser);

                fromServer = (LinkerPacket) in.readObject();
                if (fromServer != null) {
                    if (fromServer.getType() == LinkerPacket.SERVERHELLO) {
                        port = i;
                        try {
                            InetAddress inetAddress = InetAddress.getByName(ipAddress);
                            hostName = inetAddress.getCanonicalHostName();
                        } catch (Exception e) {
                        }
                    }
                    fromUser = new LinkerPacket(LinkerPacket.TERMINATE, "OK");
                    out.writeObject(fromUser);
                }

            } catch (Exception ex) {
                //Log.e(LOG_TAG, ex.getMessage());
            } finally {
                try {
                    out.close();
                } catch (Exception e) {
                }
                try {
                    in.close();
                } catch (Exception e) {
                }
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
            if (port > 0) {
                break;
            }
        }

        if (port > 0) {
            return ipAddress + ";" + port + ";" + hostName;
        } else {
            return null;
        }


    }

}
