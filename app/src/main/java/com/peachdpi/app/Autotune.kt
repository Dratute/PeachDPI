package com.peachdpi.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

object Autotune {
    private const val TEST_PORT = 1081

    @Volatile
    private var running = false

    private val SITES = listOf(
        "https://www.youtube.com/",
        "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg",
        "https://redirector.googlevideo.com/",
        "https://discord.com/"
    )

    fun run(ctx: Context, onEvent: (String) -> Unit) {
        if (running) return
        running = true
        Thread {
            try {
                work(ctx, onEvent)
            } catch (t: Throwable) {
                onEvent(event("done", "Ошибка проверки: " + (t.message ?: t.javaClass.simpleName), false))
            } finally {
                running = false
            }
        }.start()
    }

    private fun work(ctx: Context, onEvent: (String) -> Unit) {
        val total = SITES.size
        onEvent(event("progress", "Проверяю сеть без обхода…", false))
        val baseline = score(null)
        if (baseline == total) {
            onEvent(
                event(
                    "done",
                    "Без обхода открывается $total из $total: блокировка по TCP не обнаружена. " +
                        "Если YouTube всё равно не грузится, напишите мне эту строку.",
                    false
                )
            )
            return
        }
        var best = -1
        var bestScore = baseline
        val profiles = Profiles.ALL
        for (i in profiles.indices) {
            onEvent(
                event(
                    "progress",
                    "Без обхода: $baseline из $total. Проверяю набор ${i + 1} из ${profiles.size}…",
                    false
                )
            )
            var proc: Process? = null
            val sc = try {
                proc = Core.start(ctx, TEST_PORT, profiles[i])
                score(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", TEST_PORT)))
            } catch (t: Throwable) {
                -1
            } finally {
                Core.stop(proc)
            }
            if (sc > bestScore) {
                bestScore = sc
                best = i
            }
            if (sc == total) break
        }
        if (best >= 0) {
            Profiles.save(ctx, best)
            onEvent(
                event(
                    "done",
                    "Лучший набор №${best + 1}: открывается $bestScore из $total (без обхода: $baseline). Сохранено.",
                    true
                )
            )
        } else {
            onEvent(
                event(
                    "done",
                    "Ни один набор не помог: без обхода $baseline из $total, с обходом не лучше. Напишите мне эту строку.",
                    false
                )
            )
        }
    }

    private fun score(proxy: Proxy?): Int {
        val ok = AtomicInteger(0)
        val threads = SITES.map { url ->
            Thread { if (probe(url, proxy)) ok.incrementAndGet() }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        return ok.get()
    }
