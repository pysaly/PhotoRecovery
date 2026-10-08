package com.example.photorecovery.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

/** 相册读取权限的按版本处理。 */
object PermissionUtils {

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 33 ->
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else ->
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasPermissions(grants: Map<String, Boolean>): Boolean =
        requiredPermissions().all { grants[it] == true }

    fun isGranted(grantResult: Int): Boolean =
        grantResult == PackageManager.PERMISSION_GRANTED
}
