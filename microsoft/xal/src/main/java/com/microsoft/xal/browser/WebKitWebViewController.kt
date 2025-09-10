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
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class WebKitWebViewController : AppCompatActivity() {
    private var mWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        if (extras == null) {
            Log.e(TAG, "onCreate() Called with no extras.")
            setResult(RESULT_FAILED)
            finish()
            return
        }
        val url = extras.getString(START_URL, "")
        val endUrl = extras.getString(END_URL, "")
        if (url.isEmpty() || endUrl.isEmpty()) {
            Log.e(TAG, "onCreate() Received invalid start or end URL.");
            setResult(RESULT_FAILED)
            finish()
            return
        }
        val requestHeaderKeys = extras.getStringArray(REQUEST_HEADER_KEYS) ?: emptyArray()
        val requestHeaderValues = extras.getStringArray(REQUEST_HEADER_VALUES) ?: emptyArray()
        if (requestHeaderKeys.size != requestHeaderValues.size) {
            Log.e(TAG, "onCreate() Received request header and key arrays of different lengths.");
            setResult(RESULT_FAILED)
            finish()
            return
        }

        when (extras[SHOW_TYPE] as? ShowUrlType) {
            ShowUrlType.CookieRemoval, ShowUrlType.CookieRemovalSkipIfSharedCredentials -> {
                Log.i(TAG, "onCreate() WebView invoked for cookie removal. Deleting cookies and finishing.")
                if (requestHeaderKeys.isNotEmpty()) {
                    Log.w(TAG, "onCreate() WebView invoked for cookie removal with requestHeaders.")
                }
                deleteCookies("login.live.com", true)
                deleteCookies("account.live.com", true)
                deleteCookies("live.com", true)
                deleteCookies("xboxlive.com", true)
                deleteCookies("sisu.xboxlive.com", true)

                val intent = Intent()
                intent.putExtra(RESPONSE_KEY, endUrl)
                setResult(RESULT_OK, intent)
                finish()
                return
            }

            else -> {}
        }

        val hashMap = HashMap<String, String>(requestHeaderKeys.size)
        for (i in requestHeaderKeys.indices) {
            val str2 = requestHeaderKeys[i]
            val str = requestHeaderValues[i]
            if (str2.isNullOrEmpty() || str.isNullOrEmpty()) {
                Log.e(TAG, "onCreate() Received null or empty request field.")
                setResult(RESULT_FAILED)
                finish()
                return
            }
            hashMap[requestHeaderKeys[i]] = requestHeaderValues[i]
        }

        val webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, i: Int) {
                setProgress(i * 100)
            }
        }
        webView.webViewClient = XalWebViewClient(this@WebKitWebViewController, endUrl)
        webView.loadUrl(url)
        mWebView = webView
    }

    private fun deleteCookies(domain: String, useHttps: Boolean) {
        val cookieManager = CookieManager.getInstance()
        val url = (if (useHttps) {
            "https://"
        } else {
            "http://"
        }) + domain
        cookieManager.getCookie(url)?.let { cookie ->
            val isDeleted = false
            val split = cookie.split(";".toRegex()).dropLastWhile {
                it.isEmpty()
            }.toTypedArray()
            for (str2 in split) {
                val trim = str2.split("=".toRegex()).dropLastWhile {
                    it.isEmpty()
                }.toTypedArray()[0].trim()
                var str3 = "$trim=;"
                if (trim.startsWith("__Secure-")) {
                    str3 = str3 + "Secure;Domain=" + domain + ";Path=/"
                }
                cookieManager.setCookie(
                    url,
                    if (trim.startsWith("__Host-")) {
                        str3 + "Secure;Path=/"
                    } else {
                        str3 + "Domain=" + domain + ";Path=/"
                    }
                )
            }
            if (isDeleted) {
                println("deleteCookies() Deleted cookies for $domain");
            } else {
                println("deleteCookies() Found no cookies for $domain");
            }
        }
        cookieManager.flush()
    }

    companion object {
        private const val TAG = "WebKitWebViewController"
        const val END_URL = "END_URL"
        const val REQUEST_HEADER_KEYS = "REQUEST_HEADER_KEYS"
        const val REQUEST_HEADER_VALUES = "REQUEST_HEADER_VALUES"
        const val RESPONSE_KEY = "RESPONSE"
        const val RESULT_FAILED = 8054
        const val SHOW_TYPE = "SHOW_TYPE"
        const val START_URL = "START_URL"
    }
}
