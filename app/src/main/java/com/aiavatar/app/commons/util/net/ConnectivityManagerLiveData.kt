package com.aiavatar.app.commons.util.net

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Deprecated("Not working as expected")
@SuppressLint("ObsoleteSdkInt")
class ConnectivityManagerLiveData @Inject constructor(
    @ApplicationContext private val context: Context
) : LiveData<Boolean>() {

    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    private val networkRequestBuilder: NetworkRequest.Builder = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            updateConnection()
        }
    }

    init {
        updateConnection()
    }

    private fun updateConnection() {
        val activeNetworkInfo: NetworkInfo? = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
                ConnectivityManager)?.activeNetworkInfo
        postValue(context.isConnected())
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun lollipopNetworkAvailableRequest() {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
                ConnectivityManager)?.registerNetworkCallback(networkRequestBuilder.build(), getConnectivityManagerCallbackLollipop())
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun marshmallowNetworkAvailableRequest() {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
                ConnectivityManager)?.registerNetworkCallback(networkRequestBuilder.build(), getConnectivityManagerCallbackMarshMallow())
    }

    private fun getConnectivityManagerCallbackLollipop(): ConnectivityManager.NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManagerCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    postValue(true)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    postValue(false)
                }
            }
            return connectivityManagerCallback
        } else {
            throw IllegalAccessException("Invalid API version")
        }
    }

    private fun getConnectivityManagerCallbackMarshMallow(): ConnectivityManager.NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManagerCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        postValue(true)
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    postValue(false)
                }
            }
            return connectivityManagerCallback
        } else {
            throw IllegalAccessException("Invalid API version")
        }
    }

    override fun onActive() {
        super.onActive()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_USB)
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
                        ConnectivityManager)?.registerDefaultNetworkCallback(getConnectivityManagerCallbackMarshMallow())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
                        ConnectivityManager)?.registerDefaultNetworkCallback(getConnectivityManagerCallbackMarshMallow())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                marshmallowNetworkAvailableRequest()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                lollipopNetworkAvailableRequest()
            }
            else -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    context.registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
                }
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
                    ConnectivityManager)?.unregisterNetworkCallback(connectivityManagerCallback)
        } else {
            try {
                context.unregisterReceiver(networkReceiver)
            } catch (ignore: Exception) { }
        }
    }
}

/*
@Suppress("DEPRECATION")
fun Context.isConnected(): Boolean {
    val connectivityManager = (getSystemService(Context.CONNECTIVITY_SERVICE) as?
            ConnectivityManager)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        */
/* This sometimes return false even when connected to a valid wifi *//*

        val nwInfo  = connectivityManager?.activeNetworkInfo ?: return false
        if (nwInfo.type == ConnectivityManager.TYPE_WIFI) return true

        val nw      = connectivityManager.activeNetwork ?: return false
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
    } else {
        val nwInfo = connectivityManager?.activeNetworkInfo ?: return false
        return nwInfo.isConnected
    }
}*/
