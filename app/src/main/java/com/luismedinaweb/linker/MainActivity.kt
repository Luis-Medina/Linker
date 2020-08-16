package com.luismedinaweb.linker

import android.app.FragmentManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), SearchForServerTaskFragment.TaskCallbacks, VerifyConnectionTaskFragment.TaskCallbacks {

    private var prefHelper: PrefHelper? = null
    private var progressBar: ProgressBar? = null
    private var searchButton: Button? = null
    private var progressText: TextView? = null
    private var serverNameText: TextView? = null
    private var ipText: TextView? = null
    private var statusText: TextView? = null
    private val linkToSend: String? = null
    private var mSearchTaskFragment: SearchForServerTaskFragment? = null
    private var mVerifyTaskFragment: VerifyConnectionTaskFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefHelper = PrefHelper.getInstance(this)

        mSearchTaskFragment = supportFragmentManager.findFragmentByTag(SEARCH_TASK_FRAGMENT) as? SearchForServerTaskFragment
        mVerifyTaskFragment = supportFragmentManager.findFragmentByTag(VERIFY_TASK_FRAGMENT) as? VerifyConnectionTaskFragment
        initWidgets()
        setConfigTextInView()

        initFragments()

        setupListeners()
    }

    private fun initFragments() {
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        mSearchTaskFragment?.let {
            if (it.isWorking) {
                setScreenIsWorking(it.lastMessage ?: "")
            } else {
                supportFragmentManager.beginTransaction().remove(it).commit()
                mSearchTaskFragment = null
            }
        }
        if (mVerifyTaskFragment != null) {
            if (mVerifyTaskFragment!!.isWorking) {
                setScreenIsWorking("Verifying server connection")
            } else {
                serverNameText!!.text = prefHelper!!.getServerName()
                statusText!!.text = "Connected"
            }
        } else {
            mVerifyTaskFragment = VerifyConnectionTaskFragment()
            if (prefHelper!!.haveValidServer()) {
                setScreenIsWorking("Verifying server connection")
                supportFragmentManager.beginTransaction().add(mVerifyTaskFragment!!, VERIFY_TASK_FRAGMENT).commit()
                searchButton!!.isEnabled = false
            }
        }
    }

    private fun initWidgets() {
        searchButton = findViewById<View>(R.id.btn_search) as Button
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        progressText = findViewById<View>(R.id.progressText) as TextView
        serverNameText = findViewById<View>(R.id.host_name) as TextView
        statusText = findViewById<View>(R.id.lbl_connection_status) as TextView
        ipText = findViewById<View>(R.id.ipText) as TextView
    }

    private fun setScreenIsWorking(message: String) {
        searchButton!!.setText(R.string.btn_search_cancel)
        progressText!!.text = message
        progressText!!.visibility = TextView.VISIBLE
        progressBar!!.visibility = ProgressBar.VISIBLE
    }

    private fun setupListeners() {
        searchButton!!.setOnClickListener {
            if (mSearchTaskFragment != null && !mSearchTaskFragment!!.isWorking || mSearchTaskFragment == null) {
                setScreenIsWorking("")
                mSearchTaskFragment = SearchForServerTaskFragment()
                supportFragmentManager.beginTransaction().add(mSearchTaskFragment!!, SEARCH_TASK_FRAGMENT).commit()
            } else if (mSearchTaskFragment!!.isWorking) {
                searchButton!!.isEnabled = false
                progressText!!.text = "Cancelling..."
                mSearchTaskFragment!!.cancel()
            }
        }
    }

    private fun setConfigTextInView() {
        ipText!!.text = prefHelper!!.getIpAddress()
        serverNameText!!.text = prefHelper!!.getServerName()
        if (prefHelper!!.getServerPort() == PrefHelper.defaultPort) {
            ipText!!.visibility = TextView.INVISIBLE
            statusText!!.setText(R.string.lbl_disconnected_status)
            statusText!!.setTextColor(Color.RED)
        } else {
            statusText!!.setText(R.string.lbl_connected_status)
            statusText!!.setTextColor(Color.rgb(4, 141, 4))
        }
        if (prefHelper!!.getIpAddress() == prefHelper!!.getServerName()) {
            ipText!!.visibility = TextView.INVISIBLE
        } else {
            ipText!!.visibility = TextView.VISIBLE
        }
    }

    override fun onSearchProgressUpdate(message: String) {
        progressText!!.text = message
    }

    override fun onSearchPostExecute(result: Array<String>) {
        searchButton!!.setText(R.string.btn_search)
        progressBar!!.visibility = ProgressBar.INVISIBLE
        progressText!!.visibility = ProgressBar.INVISIBLE
        prefHelper!!.setIpAddress(result[0])
        prefHelper!!.setServerPort(Integer.parseInt(result[1]))
        prefHelper!!.setServerName(result[2])

        setConfigTextInView()

        if (intent.getStringExtra("url") != null) {
            val linkIntent = Intent(applicationContext, LinkActivity::class.java)
            linkIntent.action = Intent.ACTION_SEND
            linkIntent.type = "text/plain"
            linkIntent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra("url"))
            startActivity(linkIntent)
            this.finish()
        }

    }

    override fun onSearchCancelled() {
        searchButton!!.setText(R.string.btn_search)
        searchButton!!.isEnabled = true
        progressBar!!.visibility = ProgressBar.INVISIBLE
        progressText!!.visibility = ProgressBar.INVISIBLE
    }


    override fun onVerifyPostExecute(result: Boolean) {
        if ((!result)) {
            prefHelper!!.setServerName(PrefHelper.defaultServer)
            prefHelper!!.setServerPort(PrefHelper.defaultPort)
            prefHelper!!.setIpAddress(PrefHelper.defaultIP)
        }
        setConfigTextInView()
        searchButton!!.isEnabled = true
        searchButton!!.setText(R.string.btn_search)
        progressBar!!.visibility = ProgressBar.INVISIBLE
        progressText!!.visibility = ProgressBar.INVISIBLE
    }

    companion object {

        private val SEARCH_TASK_FRAGMENT = "search_task_fragment"
        private val VERIFY_TASK_FRAGMENT = "verify_task_fragment"
    }
}
