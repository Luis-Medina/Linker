package com.luismedinaweb.linker

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment

import com.google.gson.Gson
import com.google.gson.stream.JsonReader

import java.io.BufferedInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.StringReader
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.charset.Charset


/**
 * Created by Luis on 4/13/2015.
 */
class VerifyConnectionTaskFragment : Fragment() {
    private var mCallbacks: TaskCallbacks? = null
    private var mTask: VerifyConnectionTask? = null
    val isWorking = false
    private var prefHelper: PrefHelper? = null
    private val gson = Gson()

    internal interface TaskCallbacks {
        fun onVerifyPostExecute(result: Boolean)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
        prefHelper = PrefHelper.getInstance(requireActivity().applicationContext)

        // Create and execute the background task.
        mTask = VerifyConnectionTask()
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


    private inner class VerifyConnectionTask : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg voids: Void): Boolean? {
            var socket: Socket? = null
            var out: OutputStream? = null
            var `in`: BufferedInputStream? = null
            var result = false
            var reader: JsonReader? = null

            try {
                socket = Socket()
                val address = InetSocketAddress(prefHelper!!.getIpAddress(), prefHelper!!.getServerPort())
                socket.connect(address, 5000)
                out = socket.getOutputStream()
                `in` = BufferedInputStream(socket.getInputStream())

                val fromServer: String
                var fromUser: NetworkPacket

                fromUser = NetworkPacket(NetworkPacket.TYPE.CLIENT_HELLO, "Hello")
                out!!.write(gson.toJson(fromUser).toByteArray(charset("UTF-8")))

                val buffer = ByteArray(1024)
                if (`in`.read(buffer) > 0) {
                    fromServer = String(buffer, Charset.forName("UTF-8"))
                    reader = JsonReader(StringReader(fromServer))
                    reader.isLenient = true
                    val packetReceived = gson.fromJson<NetworkPacket>(reader, NetworkPacket::class.java)
                    if (packetReceived.getType() === NetworkPacket.TYPE.SERVER_HELLO) {
                        result = true
                    }
                }
                fromUser = NetworkPacket(NetworkPacket.TYPE.TERMINATE, "OK")
                out.write(gson.toJson(fromUser).toByteArray(charset("UTF-8")))
            } catch (ex: Exception) {
                Log.e(LOG_TAG, "" + ex.message)
            } finally {
                try {
                    out!!.close()
                } catch (e: Exception) {
                }

                try {
                    `in`!!.close()
                } catch (e: Exception) {
                }

                try {
                    socket!!.close()
                } catch (e: Exception) {
                }

                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "" + e.message)
                    }

                }
            }

            return result
        }

        override fun onPostExecute(result: Boolean) {
            if (mCallbacks != null) {
                mCallbacks!!.onVerifyPostExecute(result)
            }
        }

    }

    companion object {

        private val LOG_TAG = VerifyConnectionTaskFragment::class.java.simpleName
    }
}
