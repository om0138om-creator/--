package com.omarsanjaq.shareapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.*
import java.util.*

class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = mutableListOf<DeviceItem>()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var isReceiving = false

    companion object {
        private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val APP_NAME = "ShareApp"
        private const val BUFFER_SIZE = 8192
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        if (hasBluetoothPermission()) {
                            val deviceItem = DeviceItem(
                                id = it.address,
                                name = it.name ?: "Unknown Device",
                                address = it.address,
                                type = DeviceType.BLUETOOTH,
                                device = it
                            )
                            if (!discoveredDevices.any { d -> d.id == deviceItem.id }) {
                                discoveredDevices.add(deviceItem)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startDiscovery(callback: (List<DeviceItem>) -> Unit) {
        if (!hasBluetoothPermission()) {
            callback(emptyList())
            return
        }

        discoveredDevices.clear()

        // Add paired devices first
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            val deviceItem = DeviceItem(
                id = device.address,
                name = device.name ?: "Unknown Device",
                address = device.address,
                type = DeviceType.BLUETOOTH,
                device = device
            )
            discoveredDevices.add(deviceItem)
        }

        // Register receiver and start discovery
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(discoveryReceiver, filter)

        bluetoothAdapter?.startDiscovery()

        // Return results after 10 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            bluetoothAdapter?.cancelDiscovery()
            callback(discoveredDevices.toList())
        }, 10000)
    }

    fun connectToDevice(device: DeviceItem, callback: (Boolean) -> Unit) {
        if (!hasBluetoothPermission()) {
            callback(false)
            return
        }

        Thread {
            try {
                val bluetoothDevice = device.device as? BluetoothDevice
                clientSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(APP_UUID)
                bluetoothAdapter?.cancelDiscovery()
                clientSocket?.connect()
                callback(true)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
                try {
                    clientSocket?.close()
                } catch (closeException: Exception) {
                    closeException.printStackTrace()
                }
            }
        }.start()
    }

    fun sendFiles(files: List<FileItem>, callback: (Int, Boolean) -> Unit) {
        Thread {
            try {
                val outputStream = clientSocket?.outputStream
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
            }
        }.start()
    }

    fun startReceiving(callback: (FileItem?, Int) -> Unit) {
        isReceiving = true
        Thread {
            try {
                if (!hasBluetoothPermission()) {
                    return@Thread
                }

                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                val socket = serverSocket?.accept()
                clientSocket = socket

                val inputStream = socket?.inputStream
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
            context.unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        bluetoothAdapter?.cancelDiscovery()
        stopReceiving()
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == 
                PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == 
                PackageManager.PERMISSION_GRANTED
        }
    }
}
