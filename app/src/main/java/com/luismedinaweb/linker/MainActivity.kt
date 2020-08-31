package com.luismedinaweb.linker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var searchButton: Button
    private lateinit var progressText: TextView
    private lateinit var serverNameText: TextView
    private lateinit var statusText: TextView
    private val model: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initWidgets()
        setConfigTextInView()
        setupListeners()

        model.searchStatus.observe(this, { status ->
            when (status) {
                is SearchStatus.Cancelled -> handleSearchCancelled()
                is SearchStatus.Searching -> handleSearching(status.message)
                is SearchStatus.Finished -> handleSearchComplete(status.serverData)
            }
        })
    }

    private fun initWidgets() {
        searchButton = findViewById<View>(R.id.btn_search) as Button
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        progressText = findViewById<View>(R.id.progressText) as TextView
        serverNameText = findViewById<View>(R.id.host_name) as TextView
        statusText = findViewById<View>(R.id.lbl_connection_status) as TextView
    }

    private fun setupListeners() {
        searchButton.setOnClickListener {
            model.searchForServer(
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager,
                    applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            )
        }
    }

    private fun handleSearchCancelled() {
        Toast.makeText(this, "Search Cancelled", Toast.LENGTH_SHORT).show()
        searchButton.setText(R.string.btn_search)
        searchButton.isEnabled = true
        progressBar.visibility = ProgressBar.INVISIBLE
        progressText.visibility = ProgressBar.INVISIBLE
    }

    private fun handleSearching(message: String) {
        searchButton.setText(R.string.btn_search_cancel)
        progressText.text = message
        progressText.visibility = TextView.VISIBLE
        progressBar.visibility = ProgressBar.VISIBLE
    }

    private fun handleSearchComplete(result: ServerData?) {
        PrefHelper.serverData = result

        searchButton.setText(R.string.btn_search)
        progressBar.visibility = ProgressBar.INVISIBLE
        progressText.visibility = ProgressBar.INVISIBLE

        setConfigTextInView()

        if (intent.getStringExtra("url") != null && result != null) {
            val linkIntent = Intent(applicationContext, LinkActivity::class.java)
            linkIntent.action = Intent.ACTION_SEND
            linkIntent.type = "text/plain"
            linkIntent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra("url"))
            startActivity(linkIntent)
            this.finish()
        }
    }

    private fun setConfigTextInView() {
        if (PrefHelper.hasValidServer()) {
            statusText.setText(R.string.lbl_connected_status)
            statusText.setTextColor(Color.rgb(4, 141, 4))
            serverNameText.text = PrefHelper.serverData?.ipAddress
        } else {
            statusText.setText(R.string.lbl_disconnected_status)
            statusText.setTextColor(Color.RED)
        }
    }
}
