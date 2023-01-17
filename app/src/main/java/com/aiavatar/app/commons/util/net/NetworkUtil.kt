package com.aiavatar.app.commons.util.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

@Suppress("DEPRECATION")
fun Context.isConnected(): Boolean {
    val connectivityManager = (getSystemService(Context.CONNECTIVITY_SERVICE) as?
            ConnectivityManager)

    /* This sometimes return false even when connected to a valid wifi */
    /*val nwInfo  = connectivityManager?.activeNetworkInfo ?: return false
    if (nwInfo.type == ConnectivityManager.TYPE_WIFI) return true*/

    val nw      = connectivityManager?.activeNetwork ?: return false
    val actNw   = connectivityManager.getNetworkCapabilities(nw) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        //for other device how are able to connect with Ethernet
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        //for check internet over Bluetooth
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }
}