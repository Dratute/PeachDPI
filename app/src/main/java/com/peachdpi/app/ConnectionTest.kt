package com.peachdpi.app

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

object ConnectionTest {
    private val SITES = listOf(
        "Контроль (example.com)" to "https://example.com/",
        "YouTube" to "https://www.youtube.com/",
        "Discord" to "https://discord.com/"
    )

    fun run(onDone: (String) -> Unit) {
        Thread {
            val out = JSONArray()
            for ((name, url) in SITES) {
                val direct = probe(url, null)
                val viaCore = probe(url, Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 1080)))
                out.put(JSONObject().put("name", name).put("direct", direct).put("core", viaCore))
            }
            onDone(out.toString())
        }.start()
    }

    private fun probe(url: String, proxy: Proxy?): String {
        return try {
            val conn = if (proxy == null) URL(url).openConnection() else URL(url).openConnection(proxy)
            val c = conn as HttpURLConnection
            c.connectTimeout = 7000
            c.readTimeout = 7000
            c.instanceFollowRedirects = false
            c.requestMethod = "GET"
            val code = c.responseCode
            c.disconnect()
            "ok:$code"
        } catch (e: Exception) {
            "fail:" + e.javaClass.simpleName
        }
    }
}
