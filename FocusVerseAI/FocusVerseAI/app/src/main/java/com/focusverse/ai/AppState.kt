package com.focusverse.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class SessionRec(
    val start: Long,
    val end: Long,
    val durMin: Int,
    val actualMin: Int,
    val status: String, // completed | abandoned | unverified
    val goal: String,
    val enforced: Boolean
)

/** Single source of truth. Everything is stored locally on the phone (SharedPreferences). */
object AppState {
    private var sp: SharedPreferences? = null

    var onboarded by mutableStateOf(false)
    var name by mutableStateOf("")
    var cls by mutableStateOf("Class 12 / 2nd PUC")
    var exam by mutableStateOf("KCET")
    var dailyGoalMin by mutableIntStateOf(120)
    var xp by mutableIntStateOf(0)
    var dark by mutableStateOf(true)
    var notes by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var bestPop by mutableIntStateOf(0)
    var bestMatch by mutableIntStateOf(0)
    var sessions by mutableStateOf<List<SessionRec>>(emptyList())
    var blocked by mutableStateOf<Set<String>>(emptySet())
    var bookmarks by mutableStateOf<Set<String>>(emptySet())

    // Active focus session
    var focusEnd by mutableLongStateOf(0L)
    var focusStart by mutableLongStateOf(0L)
    var focusDur by mutableIntStateOf(0)
    var focusGoal by mutableStateOf("")
    var focusEnforced by mutableStateOf(false)
    var lastResult by mutableStateOf("")
    var interruptPkg by mutableStateOf<String?>(null)

    val level: Int get() = xp / 100 + 1

    fun init(ctx: Context) {
        if (sp != null) return
        sp = ctx.applicationContext.getSharedPreferences("focusverse", Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        val p = sp ?: return
        onboarded = p.getBoolean("onboarded", false)
        name = p.getString("name", "") ?: ""
        cls = p.getString("cls", "Class 12 / 2nd PUC") ?: "Class 12 / 2nd PUC"
        exam = p.getString("exam", "KCET") ?: "KCET"
        dailyGoalMin = p.getInt("goal", 120)
        xp = p.getInt("xp", 0)
        dark = p.getBoolean("dark", true)
        notes = p.getString("notes", "") ?: ""
        apiKey = p.getString("apiKey", "") ?: ""
        bestPop = p.getInt("bestPop", 0)
        bestMatch = p.getInt("bestMatch", 0)
        sessions = decode(p.getString("sessions", "[]") ?: "[]")
        blocked = (p.getStringSet("blocked", emptySet()) ?: emptySet()).toSet()
        bookmarks = (p.getStringSet("bookmarks", emptySet()) ?: emptySet()).toSet()
        focusEnd = p.getLong("fEnd", 0L)
        focusStart = p.getLong("fStart", 0L)
        focusDur = p.getInt("fDur", 0)
        focusGoal = p.getString("fGoal", "") ?: ""
        focusEnforced = p.getBoolean("fEnf", false)
        lastResult = p.getString("lastResult", "") ?: ""
    }

    fun save() {
        val p = sp ?: return
        p.edit()
            .putBoolean("onboarded", onboarded)
            .putString("name", name)
            .putString("cls", cls)
            .putString("exam", exam)
            .putInt("goal", dailyGoalMin)
            .putInt("xp", xp)
            .putBoolean("dark", dark)
            .putString("notes", notes)
            .putString("apiKey", apiKey)
            .putInt("bestPop", bestPop)
            .putInt("bestMatch", bestMatch)
            .putString("sessions", encode(sessions))
            .putStringSet("blocked", blocked)
            .putStringSet("bookmarks", bookmarks)
            .putLong("fEnd", focusEnd)
            .putLong("fStart", focusStart)
            .putInt("fDur", focusDur)
            .putString("fGoal", focusGoal)
            .putBoolean("fEnf", focusEnforced)
            .putString("lastResult", lastResult)
            .apply()
    }

    fun resetAll() {
        onboarded = false
        name = ""
        cls = "Class 12 / 2nd PUC"
        exam = "KCET"
        dailyGoalMin = 120
        xp = 0
        notes = ""
        apiKey = ""
        bestPop = 0
        bestMatch = 0
        sessions = emptyList()
        blocked = emptySet()
        bookmarks = emptySet()
        focusEnd = 0L
        focusStart = 0L
        focusDur = 0
        focusGoal = ""
        focusEnforced = false
        lastResult = ""
        interruptPkg = null
        save()
    }

    fun addXp(n: Int) {
        xp += n
        save()
    }

    // ---------- Focus session logic ----------

    fun startFocus(durMin: Int, goal: String, enforced: Boolean) {
        val now = System.currentTimeMillis()
        focusStart = now
        focusDur = durMin
        focusEnd = now + durMin * 60_000L
        focusGoal = goal
        focusEnforced = enforced
        lastResult = ""
        interruptPkg = null
        save()
    }

    fun abandonFocus() {
        if (focusEnd == 0L) return
        val now = System.currentTimeMillis()
        val actual = ((now - focusStart) / 60_000L).toInt().coerceIn(0, focusDur)
        addSession(SessionRec(focusStart, now, focusDur, actual, "abandoned", focusGoal, focusEnforced))
        lastResult = "Session ended early after $actual min. No XP this time, but those minutes still count toward today's goal. Try a shorter session next."
        clearFocus()
    }

    /**
     * Called every second by the UI and by FocusService. Returns true if it just finished a session.
     * A session is only marked "completed" if the app noticed the end within 2 minutes.
     * If the phone was off / the app was killed, it is recorded as "unverified" and earns no XP.
     */
    fun checkExpiry(): Boolean {
        if (focusEnd == 0L) return false
        val now = System.currentTimeMillis()
        if (now < focusEnd) return false
        val late = now - focusEnd
        val goalText = if (focusGoal.isBlank()) "" else " on \"$focusGoal\""
        if (late <= 120_000L) {
            addSession(SessionRec(focusStart, focusEnd, focusDur, focusDur, "completed", focusGoal, focusEnforced))
            xp += focusDur
            lastResult = "🎉 Session complete! $focusDur min of focus$goalText. +$focusDur XP. Take a 5-minute break, then pick one small next step."
        } else {
            addSession(SessionRec(focusStart, focusEnd, focusDur, 0, "unverified", focusGoal, focusEnforced))
            lastResult = "The timer ended while FocusVerse was not running, so this session could not be verified and earns no XP. Keep the app allowed to run in the background."
        }
        clearFocus()
        return true
    }

    private fun clearFocus() {
        focusEnd = 0L
        focusStart = 0L
        focusDur = 0
        focusGoal = ""
        focusEnforced = false
        interruptPkg = null
        save()
    }

    private fun addSession(s: SessionRec) {
        sessions = (sessions + s).takeLast(300)
    }

    // ---------- Stats ----------

    fun dayStartAgo(n: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_YEAR, -n)
        return c.timeInMillis
    }

    private fun dayStartOf(t: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = t
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun minutesOnDay(ago: Int): Int {
        val s = dayStartAgo(ago)
        val e = if (ago == 0) Long.MAX_VALUE else dayStartAgo(ago - 1)
        return sessions.filter { it.end in s until e }.sumOf { it.actualMin }
    }

    fun streak(): Int {
        val days = sessions.filter { it.status == "completed" }.map { dayStartOf(it.end) }.toSet()
        var i = 0
        if (dayStartAgo(0) !in days) i = 1
        var n = 0
        while (dayStartAgo(i) in days) {
            n++
            i++
        }
        return n
    }

    fun completedToday(minDur: Int): Boolean {
        val s = dayStartAgo(0)
        return sessions.any { it.status == "completed" && it.end >= s && it.durMin >= minDur }
    }

    // ---------- JSON helpers ----------

    private fun encode(list: List<SessionRec>): String {
        val a = JSONArray()
        list.forEach {
            a.put(
                JSONObject()
                    .put("s", it.start)
                    .put("e", it.end)
                    .put("d", it.durMin)
                    .put("a", it.actualMin)
                    .put("st", it.status)
                    .put("g", it.goal)
                    .put("en", it.enforced)
            )
        }
        return a.toString()
    }

    private fun decode(s: String): List<SessionRec> {
        val out = ArrayList<SessionRec>()
        try {
            val a = JSONArray(s)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(
                    SessionRec(
                        o.getLong("s"), o.getLong("e"), o.getInt("d"), o.getInt("a"),
                        o.getString("st"), o.optString("g"), o.optBoolean("en")
                    )
                )
            }
        } catch (e: Exception) {
            // ignore corrupt data
        }
        return out
    }
}
