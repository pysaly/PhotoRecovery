package com.example.photorecovery.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 恢复逻辑：
 *  - TRASH：把回收站项目从回收站移出（is_trashed = 0），回到系统图库。
 *  - DEEP ：把 root 扫描到的媒体文件复制到「下载/PhotoRecovery」目录，
 *           以便在图库/文件管理器中查看。Android 10+ 通过 MediaStore 写入。
 */
object RecoveryRepository {

    /** 恢复一个回收站项目，返回是否成功。 */
    suspend fun restoreTrashItem(context: Context, item: MediaItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 0)
            }
            val updated = resolver.update(item.uri, values, null, null)
            if (updated <= 0) throw IOException("系统拒绝恢复该项目（可能权限不足）")
        }
    }

    /** 恢复一个深度扫描项目（复制到公开目录），返回是否成功。 */
    suspend fun restoreDeepItem(context: Context, item: MediaItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val src = File(item.uri.path ?: throw IOException("无效的文件路径"))
            if (!src.isFile) throw IOException("源文件不存在")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                restoreViaMediaStore(context, src, item)
            } else {
                restoreToLegacyDirectory(context, src)
            }
        }
    }

    private fun restoreViaMediaStore(context: Context, src: File, item: MediaItem) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, src.name)
            put(MediaStore.MediaColumns.MIME_TYPE, guessMime(item.type, src))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PhotoRecovery")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("无法创建目标文件")
        resolver.openOutputStream(uri)?.use { out ->
            src.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("无法写入目标文件")

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private fun restoreToLegacyDirectory(context: Context, src: File) {
        val destDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PhotoRecovery"
        )
        if (!destDir.exists()) destDir.mkdirs()
        val dest = File(destDir, src.name)
        src.copyTo(dest, overwrite = true)
    }

    private fun guessMime(type: MediaType, src: File): String {
        val ext = src.extension.lowercase()
        return when {
            type == MediaType.VIDEO -> "video/${ext.ifEmpty { "mp4" }}"
            ext == "png" -> "image/png"
            ext == "gif" -> "image/gif"
            ext == "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
