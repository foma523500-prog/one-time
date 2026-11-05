package com.onetime.app.data

import kotlinx.serialization.Serializable

/** 计时模式 */
object TimerMode {
    const val COUNT_UP = "COUNT_UP"
    const val COUNT_DOWN = "COUNTDOWN"
}

/** 一次专注记录 */
@Serializable
data class Session(
    val id: String,
    val taskName: String,
    val startAt: Long,
    val endAt: Long,
    val minutes: Int,
    val coins: Double,
    val plannedMinutes: Int? = null,
    /** "COMPLETED" / "INTERRUPTED" */
    val status: String = STATUS_COMPLETED,
    val reason: String = "",
    val taskId: String? = null,
) {
    val completed: Boolean get() = status == STATUS_COMPLETED
    companion object {
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_INTERRUPTED = "INTERRUPTED"
    }
}

/** 子任务 */
@Serializable
data class Subtask(
    val id: String,
    val title: String,
    val minutes: Int = 25,
    val done: Boolean = false,
    val doneAt: Long? = null,
)

/** 待办（带子任务时即为「拆分目标」） */
@Serializable
data class TodoItem(
    val id: String,
    val title: String,
    val dueAt: Long,
    val done: Boolean = false,
    val createdAt: Long,
    val doneAt: Long? = null,
    val subtasks: List<Subtask> = emptyList(),
) {
    val hasSubtasks: Boolean get() = subtasks.isNotEmpty()
    val doneSubtasks: Int get() = subtasks.count { it.done }
    val totalMinutes: Int get() = subtasks.sumOf { it.minutes }
}

/** 随记 */
@Serializable
data class NoteItem(
    val id: String,
    val text: String,
    val createdAt: Long,
    val editedAt: Long? = null,
    /** 关联的专注事项 */
    val taskName: String? = null,
)

/** 商城商品 */
@Serializable
data class ShopItem(
    val id: String,
    val name: String,
    val emoji: String = "🎁",
    val price: Double,
    val stock: Int = 0,
    /** "pause" / "stop"：功能券 */
    val voucher: String? = null,
    val custom: Boolean = false,
)

/** 兑换历史 */
@Serializable
data class Redemption(
    val id: String,
    val name: String,
    val emoji: String = "🎁",
    val price: Double,
    val at: Long,
)

/** 常用时段（挂在表盘上的金色标签） */
@Serializable
data class QuickSlot(
    val id: String,
    val name: String,
    val minutes: Int,
)

/** 用户设置 */
@Serializable
data class Settings(
    val night: Boolean = true,
    /** true = 显示「分钟」，false = 显示「min」 */
    val unitMinute: Boolean = true,
    val showSeconds: Boolean = false,
    val customSlots: List<QuickSlot> = emptyList(),
    /** 待办快捷日期：几天后 */
    val quickDays: List<Int> = listOf(1, 3, 7),
    /** 是否已播放过首次指针引导 */
    val needleIntroPlayed: Boolean = false,
)

/** 运行中的计时（用于进程被杀后恢复） */
@Serializable
data class ActiveTimer(
    val mode: String,
    val label: String,
    val plannedMinutes: Int?,
    val startedAt: Long,
    val accumulatedMs: Long,
    val paused: Boolean = false,
    val pausedAt: Long = 0L,
    val pauseCount: Int = 0,
    val taskId: String? = null,
    val subtaskId: String? = null,
    val pauseReason: String = "",
)

@Serializable
data class AppData(
    val coins: Double = 0.0,
    val totalEarned: Double = 0.0,
    val seeded: Boolean = false,
    val sessions: List<Session> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val shopItems: List<ShopItem> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
    val settings: Settings = Settings(),
    val timer: ActiveTimer? = null,
)

const val PAUSE_VOUCHER_ID = "builtin-pause"
const val STOP_VOUCHER_ID = "builtin-stop"

val BUILTIN_SHOP_ITEMS = listOf(
    ShopItem(PAUSE_VOUCHER_ID, "暂停券", "⏸", 1.0, stock = 10, voucher = "pause"),
    ShopItem(STOP_VOUCHER_ID, "停止券", "⏹", 3.0, stock = 10, voucher = "stop"),
    ShopItem("builtin-coffee", "咖啡", "☕", 6.0),
    ShopItem("builtin-milktea", "奶茶", "🧋", 9.0),
    ShopItem("builtin-cake", "蛋糕", "🍰", 15.0),
    ShopItem("builtin-pudding", "布丁", "🍮", 12.0),
    ShopItem("builtin-movie", "电影", "🎬", 25.0),
    ShopItem("builtin-rest", "休息", "🛌", 20.0),
)

/** 每多少分钟 = 1 金币 */
const val MINUTES_PER_COIN = 10
const val MAX_CUSTOM_DURATIONS = 12
const val MIN_DURATION = 5
const val MAX_DURATION = 180
const val MAX_SUBTASKS = 50
const val PAUSE_GRACE_MS = 5 * 60 * 1000L
const val MAX_CUSTOM_ITEMS = 20
const val MAX_NOTE_LENGTH = 500
const val MAX_TASK_INPUT = 80
const val MAX_TODO_INPUT = 120

fun coinsForMinutes(minutes: Int): Double {
    val raw = minutes.toDouble() / MINUTES_PER_COIN
    val rounded = kotlin.math.round(raw * 10.0) / 10.0
    return if (rounded < 0.1) 0.1 else rounded
}

fun Double.coinText(): String =
    if (kotlin.math.abs(this - kotlin.math.round(this)) < 0.001) {
        this.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", this)
    }

fun AppData.voucherCount(voucher: String): Int =
    shopItems.firstOrNull { it.voucher == voucher }?.stock ?: 0
