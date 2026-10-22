package com.peachdpi.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import hev.htproxy.TProxyService
import java.io.File

object VpnState {
    @Volatile var state: String = "disconnected"
    @Volatile var message: String = ""
    @Volatile var listener: ((String, String) -> Unit)? = null

    fun set(s: String, m: String = "") {
        state = s
        message = m
        listener?.invoke(s, m)
    }
}

class PeachVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.peachdpi.app.START"
        const val ACTION_STOP = "com.peachdpi.app.STOP"
        private const val TAG = "PeachVpn"
        private const val CHANNEL_ID = "peachdpi_vpn"
        private const val NOTIF_ID = 1
        private const val PROXY_PORT = 1080
        private const val MTU = 8500
    }

    private val lock = Any()
    private var tun: ParcelFileDescriptor? = null
    private var proxy: Process? = null
    private var tunnelRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Thread {
                stopAll()
                VpnState.set("disconnected")
                Handler(Looper.getMainLooper()).post {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }.start()
            return START_NOT_STICKY
        }
        showForeground()
        Thread { startAll() }.start()
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        Thread {
            stopAll()
            VpnState.set("disconnected")
            Handler(Looper.getMainLooper()).post {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {
        stopAll()
        if (VpnState.state != "error") VpnState.set("disconnected")
        super.onDestroy()
    }

    private fun startAll() {
        synchronized(lock) {
            if (tun != null) return
            VpnState.set("connecting")
            try {
                proxy = Core.start(this, PROXY_PORT, Profiles.current(this))
                startTunnel()
                VpnState.set("connected")
            } catch (t: Throwable) {
                Log.e(TAG, "start failed", t)
                stopInternal()
                VpnState.set("error", t.message ?: "Неизвестная ошибка")
                Handler(Looper.getMainLooper()).post {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun stopAll() {
        synchronized(lock) { stopInternal() }
    }

    private fun startTunnel() {
        val b = Builder()
            .setSession("PeachDPI")
            .setMtu(MTU)
            .addAddress("10.10.10.10", 32)
            .addRoute("0.0.0.0", 0)
            .addAddress("fd00::1", 128)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
        try {
            // Само приложение и ядро идут мимо туннеля, иначе трафик зациклится
            b.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "disallow self failed", e)
        }
        val fd = b.establish()
            ?: throw IllegalStateException("Система не создала VPN-интерфейс (нет разрешения?)")
        tun = fd

        val yml = """
            tunnel:
              mtu: $MTU
              ipv4: 198.18.0.1
              ipv6: 'fc00::1'
            socks5:
              port: $PROXY_PORT
              address: 127.0.0.1
              udp: 'udp'
            misc:
              task-stack-size: 81920
              log-level: warn
        """.trimIndent()
        val cfg = File(cacheDir, "hev.yml")
        cfg.writeText(yml)

        val ok = TProxyService.TProxyStartService(cfg.absolutePath, fd.fd)
        if (!ok) throw IllegalStateException("Не удалось запустить передачу трафика (tun2socks)")
        tunnelRunning = true
    }

    private fun stopInternal() {
        if (tunnelRunning) {
            try {
                TProxyService.TProxyStopService()
            } catch (t: Throwable) {
                Log.w(TAG, "stop tunnel failed", t)
            }
            tunnelRunning = false
        }
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
        Core.stop(proxy)
        proxy = null
    }

    private fun showForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Состояние PeachDPI", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("PeachDPI")
            .setContentText("Служба запущена")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }
}
