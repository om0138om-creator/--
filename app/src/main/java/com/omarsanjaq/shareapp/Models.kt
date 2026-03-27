package com.omarsanjaq.shareapp

import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class FileItem(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val uri: Uri
) : Parcelable

data class DeviceItem(
    val id: String,
    val name: String,
    val address: String,
    val type: DeviceType,
    val device: @RawValue Any? = null,
    val wifiP2pDevice: WifiP2pDevice? = null
)

enum class DeviceType {
    BLUETOOTH,
    WIFI_DIRECT
}
