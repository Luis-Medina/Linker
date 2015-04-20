package com.luismedinaweb.linker;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import linker.LinkerPacket;

/**
 * Created by Luis on 4/13/2015.
 */
public class SendToServerTaskFragment extends Fragment {

    private TaskCallbacks mCallbacks;
    private SendToServerTask mTask;
    private boolean isWorking = false;
    private PrefHelper prefHelper;
    private static final String TAG = "SendTask";


    interface TaskCallbacks {
        void onSendPostExecute(String result);
    }

    public boolean isWorking() {
        return isWorking;
    }


    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (TaskCallbacks) activity;
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
        prefHelper = PrefHelper.getInstance(getActivity().getApplicationContext());

        // Create and execute the background task.
        mTask = new SendToServerTask();
        mTask.execute(getArguments().getString("url"));

    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private class SendToServerTask extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... urls) {
            String result = "";
            ObjectOutputStream out = null;
            Socket socket = null;
            ObjectInputStream in = null;

            try {
                socket = new Socket();
                SocketAddress address = new InetSocketAddress(prefHelper.getIpAddress(), prefHelper.getServerPort());
                socket.connect(address, 5000);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                LinkerPacket fromServer;
                LinkerPacket fromUser;

                fromUser = new LinkerPacket(LinkerPacket.LINK, urls[0]);
                out.writeObject(fromUser);

                if ((fromServer = (LinkerPacket) in.readObject()) != null) {
                    if (fromServer.getType() == LinkerPacket.ACK) {
                        result = "Link sent!";
                    }else{
                        result = fromServer.getContent();
                    }
                    fromUser = new LinkerPacket(LinkerPacket.TERMINATE, "OK");
                    out.writeObject(fromUser);
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                result = e.toString();
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
            return result;
        }

        protected void onPostExecute(String result) {
            if(mCallbacks != null){
                mCallbacks.onSendPostExecute(result);
            }
        }

    }

}
