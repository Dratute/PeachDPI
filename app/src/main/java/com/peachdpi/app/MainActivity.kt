package com.peachdpi.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var web: WebView

    companion object {
        private const val REQ_VPN = 1
        private const val REQ_NOTIF = 2
    }

    inner class NativeBridge {
        @JavascriptInterface
        fun start() {
            runOnUiThread { requestStart() }
        }

        @JavascriptInterface
        fun stop() {
            runOnUiThread { stopVpn() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bg = Color.parseColor("#120D12")
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = FrameLayout(this)
        root.setBackgroundColor(bg)

        web = WebView(this)
        web.setBackgroundColor(bg)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(NativeBridge(), "PeachAndroid")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
            override fun onPageFinished(view: WebView?, url: String?) {
                push(VpnState.state, VpnState.message)
            }
        }
        root.addView(
            web,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
            insets
        }

        setContentView(root)
        VpnState.listener = { s, m -> runOnUiThread { push(s, m) } }
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        VpnState.listener = null
        web.destroy()
        super.onDestroy()
    }

    private fun push(s: String, m: String) {
        val js = "window.onPeachState && window.onPeachState(" +
            JSONObject.quote(s) + ", " + JSONObject.quote(m) + ")"
        web.evaluateJavascript(js, null)
    }

    private fun requestStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            return
        }
        askVpn()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) askVpn()
    }

    private fun askVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQ_VPN)
        } else {
            startVpn()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN) {
            if (resultCode == RESULT_OK) {
                startVpn()
            } else {
                VpnState.set("error", "Разрешение VPN не выдано")
            }
        }
    }

    private fun startVpn() {
        VpnState.set("connecting")
        val i = Intent(this, PeachVpnService::class.java).setAction(PeachVpnService.ACTION_START)
        startForegroundService(i)
    }

    private fun stopVpn() {
        val i = Intent(this, PeachVpnService::class.java).setAction(PeachVpnService.ACTION_STOP)
        startService(i)
    }
}
