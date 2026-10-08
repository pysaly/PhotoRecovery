package com.example.photorecovery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 快速恢复扫描器：读取系统相册回收站（MediaStore IS_TRASHED）。
 * 仅 Android 11（API 30）及以上支持系统回收站。
 */
object MediaScanner {

    suspend fun scanRecycleBin(resolver: ContentResolver): List<MediaItem> = withContext(Dispatchers.IO) {
        // 回收站机制 Android 11 才引入，旧系统无法通过该接口找回
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

        val result = mutableListOf<MediaItem>()
        result += scanCollection(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.PHOTO)
        result += scanCollection(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO)
        result.sortedByDescending { it.dateTaken }
    }

    private fun scanCollection(
        resolver: ContentResolver,
        collection: Uri,
        type: MediaType
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        )
        // 只取处于回收站（已删除但未清空）的项目
        val selection = "${MediaStore.MediaColumns.IS_TRASHED} = ?"
        val selectionArgs = arrayOf("1")

        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                items += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = cursor.getString(nameIdx) ?: "media_$id",
                    size = cursor.getLong(sizeIdx),
                    dateTaken = cursor.getLong(dateIdx) * 1000L,
                    type = type,
                    source = RecoverySource.TRASH
                )
            }
        }
        return items
    }
}
