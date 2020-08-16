package com.luismedinaweb.linker

import android.app.Activity
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.TextUtils
import androidx.fragment.app.Fragment
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Luis on 4/13/2015.
 */
class SearchForServerTaskFragment : Fragment() {

    private var rangeStart: String? = null

    private var mCallbacks: TaskCallbacks? = null
    private var mTask: SearchForServerTask? = null
    var isWorking = false
        private set
    var lastMessage: String? = null
        private set

    private val networkInfo: Boolean
        get() {
            activity?.applicationContext?.let {
                var validNetwork = true
                val connMgr = it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connMgr.activeNetworkInfo
                if (networkInfo != null && networkInfo.isConnected) {
                    if (networkInfo.type == ConnectivityManager.TYPE_ETHERNET ||
                            networkInfo.type == ConnectivityManager.TYPE_WIFI ||
                            networkInfo.type == ConnectivityManager.TYPE_VPN) {
                        val wifiManager = it.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val connectionInfo = wifiManager.connectionInfo
                        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
                            val quads = ByteArray(4)
                            for (k in 0..3) {
                                quads[k] = (connectionInfo.ipAddress shr k * 8 and 0xFF).toByte()
                            }
                            try {
                                val ipAddress = InetAddress.getByAddress(quads).toString().replace("/", "")
                                rangeStart = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1)
                                validNetwork = true
                            } catch (e: UnknownHostException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
                if (!validNetwork) {
                    rangeStart = null
                    return false
                } else {
                    return true
                }
            } ?: run {
                return false
            }
        }


    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    internal interface TaskCallbacks {
        fun onSearchProgressUpdate(message: String)
        fun onSearchPostExecute(result: Array<String>)
        fun onSearchCancelled()
    }

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mCallbacks = activity as TaskCallbacks
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retain this fragment across configuration changes.
        retainInstance = true

        // Create and execute the background task.
        mTask = SearchForServerTask()
        mTask!!.execute()

    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    fun cancel() {
        try {
            mTask!!.cancel(true)
        } catch (e: Exception) {
        }

    }

    private inner class SearchForServerTask : AsyncTask<Void, String, Array<String>>() {

        internal var pool = Executors.newFixedThreadPool(10)

        override fun doInBackground(vararg voids: Void): Array<String>? {
            isWorking = true

            var result: Array<String>? = null

            if (networkInfo) {
                publishProgress("Finding available computers...")
                val nodes = readARP()
                if (nodes.isEmpty()) cancel(true)
                pool = Executors.newFixedThreadPool(10)
                val set = HashSet<Future<String>>()
                publishProgress("Scanning ports...")
                try {
                    for (ipAddress in nodes) {
                        if (!isCancelled) {
                            val callable = PortSniffer(ipAddress)
                            val future = pool.submit<String>(callable)
                            set.add(future)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Thread", e.message)
                }

                var portFound = false
                while (!(portFound || isCancelled)) {
                    for (future in set) {
                        if (!isCancelled) {
                            if (future.isDone) {
                                try {
                                    val theResult = future.get()
                                    if (theResult != null) {
                                        result = arrayOf(theResult.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                                        ,theResult.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                                        ,theResult.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2])
                                        portFound = true
                                        break
                                    }
                                } catch (e: InterruptedException) {
                                    System.err.println(e.message)
                                } catch (e: ExecutionException) {
                                    System.err.println(e.message)
                                }

                            }
                        } else {
                            break
                        }
                    }
                }

                for (future in set) {
                    future.cancel(true)
                }
                pool.shutdownNow()
                try {
                    pool.awaitTermination(5, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    Log.e("Thread", "Error while waiting PORT pool termination: " + e.message)
                }

            }

            return result
        }

        private fun readARP(): ArrayList<String> {
            var bufferedReader: BufferedReader? = null
            val ipList = ArrayList<String>()
            try {
                var lineCount = 0
                var retries = 0
                val maxRetries = 5
                val set = HashSet<Future<Any>>()

                while (retries < maxRetries && !isCancelled) {
                    //Ping each node in network
                    var i = 1
                    while (!isCancelled && i < 255) {
                        pool.submit(PingNode(rangeStart!! + i))
                        i++
                    }

                    pool.shutdown()
                    try {
                        pool.awaitTermination(5, TimeUnit.MINUTES)
                    } catch (e: InterruptedException) {
                        Log.e("Thread", "Error while awaiting ARP pool termination: " + e.message)
                    }

                    if (isCancelled) {
                        break
                    }

                    Log.d("Thread", "Ping done")

                    //Get MAC addresses from arp table
                    var noMacAddresses = true
                    bufferedReader = BufferedReader(FileReader("/proc/net/arp"))
                    //Log.d("%%%% Printing ARP Table", "See below !!!!!!!!!!!!!");
                    while(true){
                        val line = bufferedReader.readLine() ?: break

                        lineCount++
                        if (lineCount == 1) {  //Skip first line
                            continue
                        }
                        if (isCancelled) {
                            break
                        }
                        //Log.d("Arp line ----- ", line);
                        val token = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val matcher = IP_ADDRESS.matcher(token[0])
                        if (matcher.matches()) {
                            if (!token[3].equals(DEFAULT_MAC, ignoreCase = true)) {
                                ipList.add(token[0])
                                noMacAddresses = false
                            }
                        }
                    }
                    //If arp table is less than 10 lines or all MAC addresses on table are 00:00:00:00:00:00, try again
                    if (lineCount < 10 || noMacAddresses) {
                        retries++
                        lineCount = 0
                    } else {
                        retries = maxRetries + 1
                    }
                    bufferedReader.close()
                    if (isCancelled) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("Thread", e.message)
                ipList.add("192.168.0.110")
            } finally {
                try {
                    bufferedReader!!.close()
                } catch (e: Exception) {
                }

            }

            return ipList
        }

        override fun onProgressUpdate(vararg message: String) {
            lastMessage = message[0]
            if (mCallbacks != null) {
                mCallbacks!!.onSearchProgressUpdate(message[0])
            }
        }

        override fun onCancelled() {
            if (mCallbacks != null) {
                mCallbacks!!.onSearchCancelled()
            }
            isWorking = false
        }

        /* result[0] = IP Address
           result[1] = Port
           result[2] = Hostname
         */
        override fun onPostExecute(result: Array<String>) {
            if (mCallbacks != null) {
                mCallbacks!!.onSearchPostExecute(result)
            }
            isWorking = false
        }

    }

    companion object {

        val DEFAULT_MAC = "00:00:00:00:00:00"
        private val IP_ADDRESS = Pattern.compile(
                "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                        + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                        + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                        + "|[1-9][0-9]|[0-9]))")
    }


}
