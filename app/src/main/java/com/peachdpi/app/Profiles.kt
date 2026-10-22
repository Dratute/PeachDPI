package com.peachdpi.app

import android.content.Context

object Profiles {
    val ALL: List<List<String>> = listOf(
        listOf("--disorder", "1", "--auto=torst", "--tlsrec", "1+s"),
        listOf("--disorder", "1", "--tlsrec", "1+s"),
        listOf("--fake", "-1", "--ttl", "8"),
        listOf("--fake", "-1", "--ttl", "8", "--tlsrec", "1+s"),
        listOf("--split", "1+s", "--disorder", "3+s"),
        listOf("--oob", "1+s"),
        listOf("--disoob", "1+s"),
        listOf("--fake", "-1", "--md5sig"),
        listOf("--split", "2", "--tlsrec", "3+s")
    )

    private const val PREFS = "peach"
    private const val KEY = "profile"

    fun index(ctx: Context): Int {
        val i = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0)
        return if (i in ALL.indices) i else 0
    }

    fun current(ctx: Context): List<String> = ALL[index(ctx)]

    fun save(ctx: Context, i: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, i).apply()
    }
}
