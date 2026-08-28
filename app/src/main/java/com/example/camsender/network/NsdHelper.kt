package com.example.camsender.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log

class NsdHelper(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val serviceType = "_http._tcp."
    private val targetServiceName = "CamSenderServer"

    interface OnServerFoundListener {
        fun onServerFound(ip: String, port: Int, apiPath: String?, version: String?)
        fun onServerLost()
    }

    var listener: OnServerFoundListener? = null

    fun startDiscovery() {
        stopDiscovery() // Ensure clean start

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("NsdHelper", "Discovery Start Failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("NsdHelper", "Discovery Stop Failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d("NsdHelper", "Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d("NsdHelper", "Service discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == serviceType && serviceInfo.serviceName.contains(targetServiceName)) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            Log.e("NsdHelper", "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                            Log.d("NsdHelper", "Resolve Succeeded: $resolvedServiceInfo")
                            val ip = resolvedServiceInfo.host.hostAddress ?: ""
                            val port = resolvedServiceInfo.port
                            
                            // Extract TXT records (API level 21+)
                            val attributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                resolvedServiceInfo.attributes
                            } else {
                                emptyMap()
                            }
                            
                            val apiPath = attributes["api"]?.let { String(it) }
                            val version = attributes["version"]?.let { String(it) }
                            
                            listener?.onServerFound(ip, port, apiPath, version)
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Service lost: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName.contains(targetServiceName)) {
                    listener?.onServerLost()
                }
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e("NsdHelper", "Error stopping discovery", e)
            }
        }
        discoveryListener = null
    }
}
