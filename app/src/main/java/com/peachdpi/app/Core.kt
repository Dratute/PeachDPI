package com.peachdpi.app

import android.content.Context
import android.util.Log
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object Core {
    private const val TAG = "PeachCore"

    fun start(ctx: Context, port: Int, profile: List<String>): Process {
        val exe = File(ctx.applicationInfo.nativeLibraryDir, "libciadpi.so")
        if (!exe.exists()) throw IllegalStateException("Ядро обхода не найдено в приложении")
        val cmd = mutableListOf(exe.absolutePath, "-i", "127.0.0.1", "-p", port.toString())
        cmd.addAll(profile)
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        Thread {
            try {
                p.inputStream.bufferedReader().forEachLine { Log.d(TAG, it) }
            } catch (_: Exception) {
            }
        }.start()
        try {
            waitPort(p, port, 4000)
        } catch (t: Throwable) {
            stop(p)
            throw t
        }
        return p
    }

    fun stop(p: Process?) {
        if (p == null) return
        p.destroy()
        try {
            if (!p.waitFor(2, TimeUnit.SECONDS)) p.destroyForcibly()
        } catch (_: Exception) {
        }
    }

    private fun waitPort(p: Process, port: Int, timeoutMs: Long) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (!p.isAlive) {
                throw IllegalStateException("Ядро обхода сразу завершилось (код ${p.exitValue()})")
            }
            try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200) }
                return
            } catch (_: Exception) {
                Thread.sleep(150)
            }
        }
        throw IllegalStateException("Ядро обхода не открыло порт")
    }
}
