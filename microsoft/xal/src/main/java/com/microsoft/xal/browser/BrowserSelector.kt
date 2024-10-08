/*
 * Copyright (C) 2018-2022 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.microsoft.xal.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsService
import androidx.core.os.EnvironmentCompat
import org.jetbrains.annotations.Contract
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
object BrowserSelector {
    private val customTabsAllowedBrowsers = HashMap<String, String>()

    init {
        customTabsAllowedBrowsers.apply {
            put("com.android.chrome", "OJGKRT0HGZNU+LGa8F7GViztV4g=")
            put("org.mozilla.firefox", "kg9Idqale0pqL0zK9l99Kc4m/yw=")
            put("com.microsoft.emmx", "P2QOJ59jvOpxCCrn6MfvotoBTK0=")
            put("com.sec.android.app.sbrowser", "nKUXDzgZGd/gRG/NqxixmhQ7MWM=")
        }
    }

    @JvmStatic
    fun selectBrowser(context: Context, useInProcBrowser: Boolean): BrowserSelectionResult {
        val notes: String
        val userDefaultBrowserInfo = userDefaultBrowserInfo(context)
        var useCustomTabs = false
        if (useInProcBrowser) {
            notes = "inProcRequested"
        } else if (browserInfoImpliesNoUserDefault(userDefaultBrowserInfo)) {
            notes = "noDefault"
        } else {
            val packageName = userDefaultBrowserInfo.packageName
            if (!browserSupportsCustomTabs(context, packageName)) {
                Log.e("ModdedPE", "selectBrowser() Default browser does not support custom tabs.")
                notes = "CTNotSupported"
            } else if (!browserAllowedForCustomTabs(context, packageName)) {
                Log.e(
                    "ModdedPE",
                    "selectBrowser() Default browser supports custom tabs, but is not allowed."
                )
                notes = "CTSupportedButNotAllowed"
            } else {
                Log.e(
                    "ModdedPE",
                    "selectBrowser() Default browser supports custom tabs and is allowed."
                )
                notes = "CTSupportedAndAllowed"
                useCustomTabs = true
            }
        }
        return BrowserSelectionResult(userDefaultBrowserInfo, notes, useCustomTabs)
    }

    private fun userDefaultBrowserInfo(context: Context): BrowserInfo {
        var versionName: String
        val intent = Intent("android.intent.action.VIEW", Uri.parse("https://microsoft.com"))
        val resolveActivity = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return when (val packageName = resolveActivity?.activityInfo?.packageName) {
            null -> {
                Log.e("ModdedPE", "userDefaultBrowserInfo() No default browser resolved.")
                BrowserInfo("none", 0, "none")
            }

            "android" -> {
                Log.e("ModdedPE", "userDefaultBrowserInfo() System resolved as default browser.")
                BrowserInfo("android", 0, "none")
            }

            else -> {
                var versionCode = -1
                try {
                    val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                    versionCode = packageInfo.versionCode
                    versionName = packageInfo.versionName ?: "unknown"
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("ModdedPE", "userDefaultBrowserInfo() Error in getPackageInfo(): $e")
                    versionName = EnvironmentCompat.MEDIA_UNKNOWN
                }
                Log.e(
                    "ModdedPE",
                    "userDefaultBrowserInfo() Found $packageName as user's default browser."
                )
                BrowserInfo(packageName, versionCode, versionName)
            }
        }
    }

    @Contract(pure = true)
    private fun browserInfoImpliesNoUserDefault(browserInfo: BrowserInfo): Boolean {
        return browserInfo.versionCode == 0 && browserInfo.versionName == "none"
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun browserAllowedForCustomTabs(context: Context, packageName: String): Boolean {
//        val signatureBrowser = customTabsAllowedBrowsers[packageName] ?: return false
//        try {
//            val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
//            if (packageInfo == null) {
//                Log.e("ModdedPE", "No package info found for package: $packageName")
//                return false
//            }
//            packageInfo.signatures?.let { signatures->
//                for (signature in signatures) {
//                    if (hashFromSignature(signature) == signatureBrowser) {
//                        return true
//                    }
//                }
//            }
//        } catch (e: PackageManager.NameNotFoundException) {
//            Log.e("ModdedPE", "browserAllowedForCustomTabs() Error in getPackageInfo(): $e")
//        } catch (e: NoSuchAlgorithmException) {
//            Log.e("ModdedPE", "browserAllowedForCustomTabs() Error in hashFromSignature(): $e")
//        }
//        return false
        val signatureBrowser = customTabsAllowedBrowsers[packageName] ?: return false
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
            if (packageInfo == null) {
                Log.e("ModdedPE", "No package info found for package: $packageName")
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.let { signingInfo -> // Изменено на signingInfo
                    for (signature in signingInfo.apkContentsSigners) { // Используем apkContentsSigners вместо signatures
                        if (hashFromSignature(signature) == signatureBrowser) { // Преобразуем в байтовый массив
                            return true
                        }
                    }
                }
            } else {
                packageInfo.signatures?.let { signatures ->
                    for (signature in signatures) {
                        if (hashFromSignature(signature) == signatureBrowser) {
                            return true
                        }
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("ModdedPE", "browserAllowedForCustomTabs() Error in getPackageInfo(): $e")
        } catch (e: NoSuchAlgorithmException) {
            Log.e("ModdedPE", "browserAllowedForCustomTabs() Error in hashFromSignature(): $e")
        }
        return false
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun browserSupportsCustomTabs(context: Context, packageName: String): Boolean {
        val intent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        for (resolveInfo in context.packageManager.queryIntentServices(intent, 0)) {
            if (resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun hashFromSignature(signature: Signature): String {
        val messageDigest = MessageDigest.getInstance("SHA")
        messageDigest.update(signature.toByteArray())
        return Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP)
    }
}
