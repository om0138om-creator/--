package com.omarsanjaq.shareapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.omarsanjaq.shareapp.DeviceItem
import com.omarsanjaq.shareapp.DeviceType
import com.omarsanjaq.shareapp.R

class DeviceAdapter(
    private val devices: List<DeviceItem>,
    private val onDeviceClick: (DeviceItem) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceIcon: ImageView = view.findViewById(R.id.deviceIcon)
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceStatus: TextView = view.findViewById(R.id.deviceStatus)
        val connectButton: MaterialButton = view.findViewById(R.id.connectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]

        holder.deviceName.text = device.name
        holder.deviceStatus.text = "متاح"

        // Set icon based on device type
        val iconRes = when (device.type) {
            DeviceType.BLUETOOTH -> R.drawable.ic_bluetooth
            DeviceType.WIFI_DIRECT -> R.drawable.ic_wifi
        }
        holder.deviceIcon.setImageResource(iconRes)

        holder.connectButton.setOnClickListener {
            onDeviceClick(device)
        }

        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount() = devices.size
}
