package com.example.photorecovery.util

import java.io.File

/**
 * Root 检测与 su 命令执行。
 * 仅用于「深度扫描」模式。普通用户不需要 root。
 */
object RootUtils {

    /** 检查设备是否已获得 root 权限。 */
    fun hasRoot(): Boolean {
        // 1) 常见 su 二进制路径
        val suPaths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/su/bin/su",
            "/data/adb/magisk/busybox", "/data/adb/magisk"
        )
        if (suPaths.any { File(it).exists() }) return true

        // 2) 尝试实际执行 su，验证能否拿到 uid=0
        return runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            p.inputStream.bufferedReader().readLine()?.contains("uid=0") == true
        }.getOrDefault(false)
    }

    /** 以 root 身份执行命令，返回标准输出（可为空）。非 root 时返回 null。 */
    fun runAsRoot(command: String): String? {
        if (!hasRoot()) return null
        return runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            out
        }.getOrNull()
    }
}
