package com.example.photorecovery.data

import android.net.Uri
import android.os.Environment
import com.example.photorecovery.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 深度扫描器（需 root）。
 *
 * 原理与限制说明（真实情况）：
 *  - 现代 Android 系统在删除文件后，文件系统会释放 inode 并可能被 TRIM 清空底层块，
 *    因此「已删除文件」的物理恢复成功率很低、且高度依赖设备与分区。
 *  - 本扫描以 root 身份读取整个外部存储（含被沙盒隔离、普通权限读不到的目录），
 *    枚举所有仍可访问的媒体文件，供用户迁移/备份到「照片恢复」目录——这是 root 模式
 *    下稳定、真实可运行的能力。
 *  - 若后续需要块级扫描（读取 /dev/block 按 JPEG/PNG 头签名找已删除数据块），可在此
 *    扩展 DeepScan.findDeletedBlocks()，但需处理分区大小与 TRIM 等现实限制。
 */
object DeepRootScanner {

    private val mediaExtensions = arrayOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", // 照片
        "mp4", "3gp", "mkv", "mov", "avi", "webm"   // 视频
    )

    suspend fun scanDeep(): List<MediaItem> = withContext(Dispatchers.IO) {
        if (!RootUtils.hasRoot()) throw IllegalStateException("ROOT_NOT_AVAILABLE")

        val rootDir = Environment.getExternalStorageDirectory().absolutePath
        val findExpr = mediaExtensions.joinToString(" -o ") { ext ->
            "-iname '*.$ext'"
        }
        val command = "find $rootDir -type f \\( $findExpr \\) 2>/dev/null"

        val output = RootUtils.runAsRoot(command) ?: return@withContext emptyList()
        parsePaths(output)
    }

    private fun parsePaths(raw: String): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { path ->
                runCatching {
                    val file = File(path)
                    if (!file.isFile || file.length() <= 0L) return@runCatching
                    val type = when (file.extension.lowercase()) {
                        in arrayOf("mp4", "3gp", "mkv", "mov", "avi", "webm") -> MediaType.VIDEO
                        else -> MediaType.PHOTO
                    }
                    items += MediaItem(
                        id = file.absolutePath.hashCode().toLong(),
                        uri = Uri.fromFile(file),
                        displayName = file.name,
                        size = file.length(),
                        dateTaken = file.lastModified(),
                        type = type,
                        source = RecoverySource.DEEP
                    )
                }
            }
        return items.sortedByDescending { it.dateTaken }
    }
}
