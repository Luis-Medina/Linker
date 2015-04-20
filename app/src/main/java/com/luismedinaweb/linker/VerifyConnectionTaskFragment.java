package com.luismedinaweb.linker;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import linker.LinkerPacket;

/**
 * Created by Luis on 4/13/2015.
 */
public class VerifyConnectionTaskFragment extends Fragment {

    private TaskCallbacks mCallbacks;
    private VerifyConnectionTask mTask;
    private boolean isWorking = false;
    private PrefHelper prefHelper;

    public boolean isWorking() {
        return isWorking;
    }

    interface TaskCallbacks {
        void onVerifyPostExecute(Boolean result);
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
        mTask = new VerifyConnectionTask();
        mTask.execute();

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


    private class VerifyConnectionTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            boolean result = false;

            try {
                socket = new Socket();
                SocketAddress address = new InetSocketAddress(prefHelper.getIpAddress(), prefHelper.getServerPort());
                socket.connect(address, 5000);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                LinkerPacket fromServer;
                LinkerPacket fromUser;

                fromUser = new LinkerPacket(LinkerPacket.CLIENTHELLO, "Hello");
                out.writeObject(fromUser);

                fromServer = (LinkerPacket) in.readObject();
                if (fromServer != null) {
                    if (fromServer.getType() == LinkerPacket.SERVERHELLO) {
                        result = true;
                    }
                    fromUser = new LinkerPacket(LinkerPacket.TERMINATE, "OK");
                    out.writeObject(fromUser);
                }

            } catch (ClassNotFoundException | IOException ex) {
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

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mCallbacks != null) {
                mCallbacks.onVerifyPostExecute(result);
            }
        }

    }
}
