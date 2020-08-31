package com.luismedinaweb.linker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity


class LinkActivity : AppCompatActivity() {

    private val viewModel: LinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val msgText = findViewById<View>(R.id.txt_message) as TextView
        val setupButton = findViewById<View>(R.id.btn_setup) as Button

        viewModel.result.observe(this, {
            handleResult(it)
        })

        val intent = intent
        val action = intent.action
        val type = intent.type
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (url != null && Intent.ACTION_SEND == action && type == "text/plain") {
            PrefHelper.serverData?.let {
                msgText.text = "Sending link to " + it.ipAddress
                setupButton.visibility = Button.GONE
                viewModel.sendLinkToServer(url, it.ipAddress, it.port)
            } ?: run {
                msgText.text = "Please setup a host computer"
                setupButton.visibility = Button.VISIBLE
                setupButton.setOnClickListener {
                    val mainActivity = Intent(applicationContext, MainActivity::class.java)
                    mainActivity.putExtra("url", url)
                    startActivity(mainActivity)
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun handleResult(result: LinkViewModel.LinkResult) {
        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        this.finish()
    }
}
