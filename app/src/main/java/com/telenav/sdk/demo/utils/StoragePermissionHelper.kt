package com.telenav.sdk.demo.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object StoragePermissionHelper {

    fun hasStoragePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission(activity: Activity, manageStorageRequestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            try {
                activity.startActivityForResult(intent, manageStorageRequestCode)
            } catch (e: ActivityNotFoundException) {
                activity.startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                    manageStorageRequestCode
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                manageStorageRequestCode
            )
        }
    }
}
