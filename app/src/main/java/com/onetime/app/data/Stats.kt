package com.onetime.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

val DATE_FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
val TIME_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun today(): LocalDate = LocalDate.now()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatClockTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIME_HM)

fun formatStamp(epochMillis: Long): String {
    val t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return "${"%02d".format(t.monthValue)}/${"%02d".format(t.dayOfMonth)} ${TIME_HM.format(t)}"
}

/** 顶栏：2026/11/05 08:52（可选带秒） */
fun formatTopBar(now: Long, showSeconds: Boolean): String {
    val t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val base = "${t.year}/${"%02d".format(t.monthValue)}/${"%02d".format(t.dayOfMonth)} ${TIME_HM.format(t)}"
    return if (showSeconds) "$base:${"%02d".format(t.second)}" else base
}

/** 日期选择器展示：11/05/2026 */
fun formatDueDate(epochMillis: Long): String {
    val t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return "%02d/%02d/%04d".format(t.monthValue, t.dayOfMonth, t.year)
}

/** 00:05 / 1:02:03 */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatMinutes(minutes: Int): String = if (minutes >= 60) {
    val h = minutes / 60
    val m = minutes % 60
    if (m == 0) "${h} 小时" else "$h 小时 $m 分"
} else {
    "$minutes 分钟"
}

enum class ReviewRange(val label: String) {
    TODAY("今天"),
    LAST_7_DAYS("近 7 天"),
    THIS_WEEK("本周"),
    THIS_MONTH("本月"),
}

fun ReviewRange.startDate(todayDate: LocalDate = today()): LocalDate = when (this) {
    ReviewRange.TODAY -> todayDate
    ReviewRange.LAST_7_DAYS -> todayDate.minusDays(6)
    ReviewRange.THIS_WEEK -> todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ReviewRange.THIS_MONTH -> todayDate.withDayOfMonth(1)
}

data class RangeStats(
    val focusMinutes: Int = 0,
    val completedCount: Int = 0,
    val interruptedCount: Int = 0,
    val coinsEarned: Double = 0.0,
    val coinsSpent: Double = 0.0,
) {
    val coinsNet: Double get() = coinsEarned - coinsSpent
}

fun AppData.statsIn(range: ReviewRange, todayDate: LocalDate = today()): RangeStats {
    val from = range.startDate(todayDate).startOfDayMillis()
    val to = todayDate.plusDays(1).startOfDayMillis()
    val rs = sessions.filter { it.endAt in from until to }
    val doneTodos = todos.filter { it.doneAt != null && it.doneAt!! in from until to }
    val spent = redemptions.filter { it.at in from until to }.sumOf { it.price }
    return RangeStats(
        focusMinutes = rs.sumOf { it.minutes },
        completedCount = rs.count { it.completed } + doneTodos.size,
        interruptedCount = rs.count { !it.completed },
        coinsEarned = rs.sumOf { it.coins },
        coinsSpent = spent,
    )
}

/** 区间内每日专注分钟 */
fun AppData.dailyFocus(range: ReviewRange, todayDate: LocalDate = today()): List<Pair<LocalDate, Int>> {
    val from = range.startDate(todayDate)
    val days = when (range) {
        ReviewRange.TODAY -> 1
        ReviewRange.LAST_7_DAYS -> 7
        ReviewRange.THIS_WEEK -> (todayDate.toEpochDay() - from.toEpochDay()).toInt() + 1
        ReviewRange.THIS_MONTH -> todayDate.dayOfMonth
    }
    return (0 until days.coerceAtLeast(1)).map { i ->
        val d = from.plusDays(i.toLong())
        val start = d.startOfDayMillis()
        val end = d.plusDays(1).startOfDayMillis()
        d to sessions.filter { it.endAt in start until end }.sumOf { it.minutes }
    }
}

fun AppData.completedTodosIn(range: ReviewRange, todayDate: LocalDate = today()): List<TodoItem> {
    val from = range.startDate(todayDate).startOfDayMillis()
    val to = todayDate.plusDays(1).startOfDayMillis()
    return todos
        .filter { it.done && it.doneAt != null && it.doneAt!! in from until to }
        .sortedByDescending { it.doneAt }
}

fun AppData.recordsIn(range: ReviewRange, todayDate: LocalDate = today()): List<Session> {
    val from = range.startDate(todayDate).startOfDayMillis()
    val to = todayDate.plusDays(1).startOfDayMillis()
    return sessions.filter { it.endAt in from until to }.sortedByDescending { it.endAt }
}

fun AppData.todayCoins(): Double {
    val start = today().startOfDayMillis()
    val end = today().plusDays(1).startOfDayMillis()
    return sessions.filter { it.endAt in start until end }.sumOf { it.coins }
}

fun AppData.todayCompletedCount(): Int {
    val start = today().startOfDayMillis()
    val end = today().plusDays(1).startOfDayMillis()
    return sessions.count { it.endAt in start until end } +
        todos.count { it.doneAt != null && it.doneAt!! in start until end }
}

/** 今天到期且未完成的待办（今日任务选择器用） */
fun AppData.todayOpenTodos(): List<TodoItem> {
    val start = today().startOfDayMillis()
    val end = today().plusDays(1).startOfDayMillis()
    return todos.filter { !it.done && it.dueAt in start until end }
}

/** 高频任务 */
fun AppData.frequentTasks(range: ReviewRange, limit: Int = 3): List<Pair<String, Int>> {
    val from = range.startDate().startOfDayMillis()
    val to = today().plusDays(1).startOfDayMillis()
    return sessions
        .filter { it.endAt in from until to && it.taskName.isNotBlank() }
        .groupBy { it.taskName }
        .map { (name, list) -> name to list.sumOf { it.minutes } }
        .sortedByDescending { it.second }
        .take(limit)
}
