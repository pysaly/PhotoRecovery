package com.example.photorecovery.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import com.example.photorecovery.data.MediaItem
import com.example.photorecovery.data.RecoverySource

/** 加载列表缩略图。TRASH 项走系统缩略图，DEEP 项直接解码本地文件。 */
object ThumbnailLoader {

    fun load(resolver: ContentResolver, item: MediaItem): Bitmap? {
        return when (item.source) {
            RecoverySource.TRASH -> loadTrashThumbnail(resolver, item.uri)
            RecoverySource.DEEP -> loadFileThumbnail(item.uri)
        }
    }

    private fun loadTrashThumbnail(resolver: ContentResolver, uri: Uri): Bitmap? = runCatching {
        resolver.loadThumbnail(uri, Size(256, 256), null)
    }.getOrNull()

    private fun loadFileThumbnail(uri: Uri): Bitmap? {
        val path = uri.path ?: return null
        return runCatching {
            BitmapFactory.decodeFile(path)?.let { bmp ->
                val max = 512
                val scale = if (bmp.width > max || bmp.height > max) {
                    max.toFloat() / maxOf(bmp.width, bmp.height)
                } else 1f
                Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true)
            }
        }.getOrNull()
    }
}
