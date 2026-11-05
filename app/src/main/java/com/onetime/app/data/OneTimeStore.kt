package com.onetime.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 极简本地存储：整个 AppData 序列化成一个 JSON 文件。
 * 数据量很小（几百条记录以内），这个方案最简单也最稳，不需要数据库。
 */
class OneTimeStore(context: Context) {

    private val file = File(context.filesDir, "onetime.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppData {
        val raw = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
        if (raw.isNullOrBlank()) return AppData()
        return runCatching { json.decodeFromString<AppData>(raw) }
            .getOrElse {
                // 文件损坏时先备份，再以空数据启动，避免崩溃循环
                runCatching { file.copyTo(File(file.parentFile, "onetime.corrupt.json"), overwrite = true) }
                AppData()
            }
    }

    fun save(data: AppData) {
        runCatching {
            val tmp = File(file.parentFile, "onetime.json.tmp")
            tmp.writeText(json.encodeToString(data))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}
