package com.example.photorecovery.data

import android.net.Uri

/** 媒体类型 */
enum class MediaType { PHOTO, VIDEO }

/** 数据来源：系统回收站 / root 深度扫描 */
enum class RecoverySource { TRASH, DEEP }

/**
 * 一个待恢复的媒体项目。
 *  - TRASH：来自系统相册回收站（MediaStore IS_TRASHED），uri 为 content://
 *  - DEEP ：来自 root 深度全盘扫描，uri 为 file://
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val dateTaken: Long,
    val type: MediaType,
    val source: RecoverySource
) {
    /** 用于选中集合的唯一键 */
    val key: String get() = uri.toString()
}
