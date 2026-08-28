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
                Log.e("NsdHelper", "DEBUG: Discovery Start Failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("NsdHelper", "DEBUG: Discovery Stop Failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d("NsdHelper", "DEBUG: Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d("NsdHelper", "DEBUG: Service discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // [CRITICAL DEBUG LOG] Show everything found on the network
                Log.d("NsdHelper", "DEBUG: Discovered -> Name: '${serviceInfo.serviceName}', Type: '${serviceInfo.serviceType}'")
                
                val normalizedFoundType = serviceInfo.serviceType.trim('.')
                val normalizedTargetType = serviceType.trim('.')

                if (normalizedFoundType.contains(normalizedTargetType)) {
                    Log.d("NsdHelper", "DEBUG: Type match! Checking name for '$targetServiceName'...")
                    if (serviceInfo.serviceName.contains(targetServiceName)) {
                        Log.d("NsdHelper", "DEBUG: Name match! Resolving service...")
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                                Log.e("NsdHelper", "DEBUG: Resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                                Log.d("NsdHelper", "DEBUG: Resolve Succeeded: ${resolvedServiceInfo.host}:${resolvedServiceInfo.port}")
                                val ip = resolvedServiceInfo.host.hostAddress ?: ""
                                val port = resolvedServiceInfo.port
                                
                                val attributes = resolvedServiceInfo.attributes
                                val apiPath = attributes["api"]?.let { String(it) }
                                val version = attributes["version"]?.let { String(it) }
                                
                                listener?.onServerFound(ip, port, apiPath, version)
                            }
                        })
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "DEBUG: Service lost: ${serviceInfo.serviceName}")
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
                Log.e("NsdHelper", "DEBUG: Error stopping discovery", e)
            }
        }
        discoveryListener = null
    }
}
