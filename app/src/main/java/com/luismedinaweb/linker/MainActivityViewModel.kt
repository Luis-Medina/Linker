package com.luismedinaweb.linker

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.luismedinaweb.LinkerPacket
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

class MainActivityViewModel : ViewModel() {

    private val _searchStatus: MutableLiveData<SearchStatus> = MutableLiveData(SearchStatus.NotStarted)
    private var searchScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    val searchStatus: LiveData<SearchStatus> = _searchStatus

    init {
        verifyServer()
    }

    private fun verifyServer() {
        val serverData: ServerData = PrefHelper.serverData ?: return

        searchScope.launch {
            val socket = Socket()
            var result = false
            try {
                socket.use {
                    val address = InetSocketAddress(serverData.ipAddress, serverData.port)
                    it.connect(address, 1000)

                    // Say hello to server
                    it.getOutputStream().sayHello()

                    // Read server response
                    val response = gson.fromJson(it.getInputStream().bufferedReader(), LinkerPacket::class.java)
                    result = response.type == LinkerPacket.SERVERHELLO
                }
            } catch (ex: Exception) {
                Log.e(this::class.java.simpleName, "" + ex.message)
            }
            _searchStatus.postValue(SearchStatus.Finished(if (result) PrefHelper.serverData else null))
        }
    }

    fun searchForServer(wm: WifiManager?, connectivityManager: ConnectivityManager?) {
        if (_searchStatus.value is SearchStatus.Searching) {
            searchScope.cancel()
            _searchStatus.postValue(SearchStatus.Cancelled)
        } else {
            _searchStatus.postValue(SearchStatus.Searching("Searching for server"))
            var result: ServerData? = null
            CoroutineScope(Dispatchers.Default).launch {
                val parentJob = SupervisorJob()
                val nodes = getNodes(wm)
                val jobs = nodes.map { SupervisorJob(parentJob) }
                searchScope = CoroutineScope(Dispatchers.IO + parentJob)
                searchScope.launch {
                    nodes.forEachIndexed { index, ipAddress ->
                        ensureActive()
                        val childJob = jobs[index]
                        CoroutineScope(childJob).launch {
                            val callable = PortSniffer(ipAddress, connectivityManager)
                            val serverData = callable.call(this)
                            if (serverData != null) {
                                result = serverData
                                PrefHelper.serverData = serverData
                                searchScope.cancel()
                            }
                            childJob.complete()
                        }
                    }
                }
                jobs.joinAll()
                _searchStatus.postValue(SearchStatus.Finished(result))
            }
        }
    }

    private fun getNodes(wm: WifiManager?): List<String> {
        val ipList = mutableListOf<String>()
        val start = 0
        val end = 255
        val ipAddress = wm?.connectionInfo?.ipAddress
        ipAddress?.let {
            val formattedAddress = String.format("%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)
            val subnet = formattedAddress.substringBeforeLast(".")
            val thisLastOctet = formattedAddress.substringAfterLast(".").toInt()
            var exhausted = false
            var i = 1
            while (!exhausted) {
                if (thisLastOctet - i < start && thisLastOctet + i > end) {
                    exhausted = true
                } else {
                    val previous = thisLastOctet - i
                    if (previous >= start) {
                        ipList.add("$subnet.$previous")
                    }
                    val next = thisLastOctet + i
                    if (next <= end) {
                        ipList.add("$subnet.$next")
                    }
                    i++
                }
            }
        }

        return ipList
    }

    override fun onCleared() {
        super.onCleared()
        searchScope.cancel()
    }
}