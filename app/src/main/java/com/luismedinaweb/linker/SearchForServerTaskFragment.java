package com.luismedinaweb.linker;

import android.app.Activity;
import android.app.Fragment;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Luis on 4/13/2015.
 */
public class SearchForServerTaskFragment extends Fragment {

    public final static String DEFAULT_MAC = "00:00:00:00:00:00";
    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");

    private String rangeStart;


    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface TaskCallbacks {
        void onSearchProgressUpdate(String message);
        void onSearchPostExecute(String[] result);
        void onSearchCancelled();
    }

    private TaskCallbacks mCallbacks;
    private SearchForServerTask mTask;
    private Context mContext;
    private boolean isWorking = false;
    private String lastMessage;

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

        mContext = getActivity().getApplicationContext();

        // Create and execute the background task.
        mTask = new SearchForServerTask();
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

    public void cancel(){
        try{
            mTask.cancel(true);
        }catch(Exception e){
        }
    }

    public boolean isWorking() {
        return isWorking;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    private boolean getNetworkInfo() {
        boolean validNetwork = true;
        ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET ||
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                    networkInfo.getType() == ConnectivityManager.TYPE_VPN) {
                final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                    byte[] quads = new byte[4];
                    for (int k = 0; k < 4; k++) {
                        quads[k] = (byte) ((connectionInfo.getIpAddress() >> k * 8) & 0xFF);
                    }
                    try {
                        String ipAddress = InetAddress.getByAddress(quads).toString().replace("/", "");
                        rangeStart = ipAddress.toString().substring(0, ipAddress.lastIndexOf(".") + 1);
                        validNetwork = true;
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (!validNetwork) {
            rangeStart = null;
            return false;
        } else {
            return true;
        }
    }

    private class SearchForServerTask extends AsyncTask<Void, String, String[]> {

        ExecutorService pool = Executors.newFixedThreadPool(10);

        @Override
        protected String[] doInBackground(Void... voids) {
            isWorking = true;

            String[] result = null;

            if (getNetworkInfo()) {
                publishProgress("Finding available computers...");
                ArrayList<String> nodes = readARP();
                pool = Executors.newFixedThreadPool(10);
                Set<Future<String>> set = new HashSet<Future<String>>();
                publishProgress("Scanning ports...");
                try {
                    for (String ipAddress : nodes) {
                        if (!isCancelled()) {
                            Callable<String> callable = new PortSniffer(ipAddress);
                            Future<String> future = pool.submit(callable);
                            set.add(future);
                        }
                    }
                } catch (Exception e) {
                    Log.e("Thread", e.getMessage());
                }

                boolean portFound = false;
                while (!(portFound || isCancelled())) {
                    for (Future<String> future : set) {
                        if (!isCancelled()) {
                            if (future.isDone()) {
                                try {
                                    String theResult = future.get();
                                    if (theResult != null) {
                                        result = new String[3];
                                        result[0] = theResult.split(";")[0];
                                        result[1] = theResult.split(";")[1];
                                        result[2] = theResult.split(";")[2];
                                        portFound = true;
                                        break;
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    System.err.println(e.getMessage());
                                }
                            }
                        } else {
                            break;
                        }
                    }
                }

                for (Future<String> future : set) {
                    future.cancel(true);
                }
                pool.shutdownNow();
                try {
                    pool.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e("Thread", "Error while waiting PORT pool termination: " + e.getMessage());
                }
            }

            return result;
        }

        private ArrayList<String> readARP() {
            BufferedReader bufferedReader = null;
            ArrayList<String> ipList = new ArrayList<String>();
            try {
                int lineCount = 0;
                int retries = 0;
                int maxRetries = 5;
                Set<Future<Object>> set = new HashSet<Future<Object>>();

                while ((retries < maxRetries) && !isCancelled()) {
                    //Ping each node in network
                    for (int i = 1; !isCancelled() && i < 255; i++) {
                        pool.submit(new PingNode(rangeStart + i));
                    }

                    pool.shutdown();
                    try {
                        pool.awaitTermination(5, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        Log.e("Thread", "Error while awaiting ARP pool termination: " + e.getMessage());
                    }

                    if(isCancelled()){
                        break;
                    }

                    Log.d("Thread", "Ping done");

                    //Get MAC addresses from arp table
                    String line;
                    boolean noMacAddresses = true;
                    bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));
                    //Log.d("%%%% Printing ARP Table", "See below !!!!!!!!!!!!!");
                    while ((line = bufferedReader.readLine()) != null) {
                        lineCount++;
                        if (lineCount == 1) {  //Skip first line
                            continue;
                        }
                        if (isCancelled()) {
                            break;
                        }
                        //Log.d("Arp line ----- ", line);
                        String[] token = line.split("\\s+");
                        Matcher matcher = IP_ADDRESS.matcher(token[0]);
                        if (matcher.matches()) {
                            if (!token[3].equalsIgnoreCase(DEFAULT_MAC)) {
                                ipList.add(token[0]);
                                noMacAddresses = false;
                            }
                        }
                    }
                    //If arp table is less than 10 lines or all MAC addresses on table are 00:00:00:00:00:00, try again
                    if (lineCount < 10 || noMacAddresses) {
                        retries++;
                        lineCount = 0;
                    } else {
                        retries = maxRetries + 1;
                    }
                    bufferedReader.close();
                    if (isCancelled()) {
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e("Thread", e.getMessage());
            } finally {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                }
            }

            return ipList;
        }

        @Override
        protected void onProgressUpdate(String... message) {
            lastMessage = message[0];
            if (mCallbacks != null) {
                mCallbacks.onSearchProgressUpdate(message[0]);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onSearchCancelled();
            }
            isWorking = false;
        }

        /* result[0] = IP Address
           result[1] = Port
           result[2] = Hostname
         */
        @Override
        protected void onPostExecute(String[] result) {
            if(mCallbacks != null){
                mCallbacks.onSearchPostExecute(result);
            }
            isWorking = false;
        }

    }


}
