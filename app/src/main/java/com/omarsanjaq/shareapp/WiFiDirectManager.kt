package com.omarsanjaq.shareapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WiFiDirectManager(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager? = 
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private val discoveredDevices = mutableListOf<DeviceItem>()
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var isReceiving = false

    companion object {
        private const val SERVER_PORT = 8888
        private const val BUFFER_SIZE = 8192
    }

    init {
        channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        discoveredDevices.clear()
        peerList.deviceList.forEach { device ->
            val deviceItem = DeviceItem(
                id = device.deviceAddress,
                name = device.deviceName,
                address = device.deviceAddress,
                type = DeviceType.WIFI_DIRECT,
                wifiP2pDevice = device
            )
            discoveredDevices.add(deviceItem)
        }
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        // Handle connection info
        if (info.groupFormed) {
            if (info.isGroupOwner) {
                // This device is group owner (server)
                startServer()
            } else {
                // This device is client
                val hostAddress = info.groupOwnerAddress.hostAddress
                // Connect to server
            }
        }
    }

    private val wifiP2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    // Handle WiFi P2P state change
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (hasWifiPermission()) {
                        wifiP2pManager?.requestPeers(channel, peerListListener)
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    if (hasWifiPermission()) {
                        wifiP2pManager?.requestConnectionInfo(channel, connectionInfoListener)
                    }
                }
            }
        }
    }

    fun discoverPeers(callback: (List<DeviceItem>) -> Unit) {
        if (!hasWifiPermission()) {
            callback(emptyList())
            return
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(wifiP2pReceiver, intentFilter)

        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    callback(discoveredDevices.toList())
                }, 5000)
            }

            override fun onFailure(reason: Int) {
                callback(emptyList())
            }
        })
    }

    fun connectToDevice(device: DeviceItem, callback: (Boolean) -> Unit) {
        if (!hasWifiPermission()) {
            callback(false)
            return
        }

        val wifiDevice = device.wifiP2pDevice ?: run {
            callback(false)
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = wifiDevice.deviceAddress
        }

        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                callback(true)
            }

            override fun onFailure(reason: Int) {
                callback(false)
            }
        })
    }

    fun sendFiles(files: List<FileItem>, callback: (Int, Boolean) -> Unit) {
        Thread {
            try {
                wifiP2pManager?.requestConnectionInfo(channel) { info ->
                    if (info.groupFormed) {
                        val hostAddress = info.groupOwnerAddress.hostAddress ?: return@requestConnectionInfo
                        
                        Thread {
                            try {
                                clientSocket = Socket()
                                clientSocket?.connect(InetSocketAddress(hostAddress, SERVER_PORT), 5000)
                                
                                val outputStream = clientSocket?.getOutputStream()
                                val dataOutputStream = DataOutputStream(outputStream)

                                // Send number of files
                                dataOutputStream.writeInt(files.size)

                                files.forEachIndexed { index, file ->
                                    // Send file metadata
                                    dataOutputStream.writeUTF(file.name)
                                    dataOutputStream.writeLong(file.size)

                                    // Send file content
                                    val fileInputStream = FileInputStream(File(file.path))
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var bytesRead: Int
                                    var totalBytesRead = 0L

                                    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                                        dataOutputStream.write(buffer, 0, bytesRead)
                                        totalBytesRead += bytesRead
                                        val progress = ((totalBytesRead * 100) / file.size).toInt()
                                        callback(progress, false)
                                    }

                                    fileInputStream.close()
                                }

                                dataOutputStream.flush()
                                callback(100, true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(0, false)
                            } finally {
                                clientSocket?.close()
                            }
                        }.start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(0, false)
            }
        }.start()
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                // Server is ready to accept connections
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun startReceiving(callback: (FileItem?, Int) -> Unit) {
        isReceiving = true
        Thread {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                val socket = serverSocket?.accept()
                clientSocket = socket

                val inputStream = socket?.getInputStream()
                val dataInputStream = DataInputStream(inputStream)

                // Receive number of files
                val numberOfFiles = dataInputStream.readInt()

                repeat(numberOfFiles) {
                    if (!isReceiving) return@repeat

                    // Receive file metadata
                    val fileName = dataInputStream.readUTF()
                    val fileSize = dataInputStream.readLong()

                    // Create file in downloads directory
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val file = File(downloadsDir, fileName)

                    // Receive file content
                    val fileOutputStream = FileOutputStream(file)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (totalBytesRead < fileSize) {
                        bytesRead = dataInputStream.read(buffer, 0, 
                            Math.min(buffer.size.toLong(), fileSize - totalBytesRead).toInt())
                        if (bytesRead == -1) break
                        fileOutputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        val progress = ((totalBytesRead * 100) / fileSize).toInt()
                        callback(null, progress)
                    }

                    fileOutputStream.close()

                    // Notify about received file
                    val fileItem = FileItem(
                        id = System.currentTimeMillis(),
                        name = fileName,
                        path = file.absolutePath,
                        size = fileSize,
                        mimeType = "application/octet-stream",
                        uri = android.net.Uri.fromFile(file)
                    )
                    callback(fileItem, 100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun stopReceiving() {
        isReceiving = false
        try {
            serverSocket?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(wifiP2pReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        stopReceiving()
        if (hasWifiPermission()) {
            wifiP2pManager?.removeGroup(channel, null)
        }
    }

    private fun hasWifiPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == 
                PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED
        }
    }
}
