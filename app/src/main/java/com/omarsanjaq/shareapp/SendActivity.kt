package com.omarsanjaq.shareapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.omarsanjaq.shareapp.adapters.DeviceAdapter
import com.omarsanjaq.shareapp.adapters.FileAdapter

class SendActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var transferMethodGroup: RadioGroup
    private lateinit var bluetoothRadio: RadioButton
    private lateinit var wifiRadio: RadioButton
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var selectFilesButton: MaterialButton
    private lateinit var sendButton: MaterialButton
    private lateinit var searchProgress: ProgressBar

    private lateinit var fileAdapter: FileAdapter
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var wifiDirectManager: WiFiDirectManager

    private val selectedFiles = mutableListOf<FileItem>()
    private val discoveredDevices = mutableListOf<DeviceItem>()

    companion object {
        const val FILE_PICKER_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        initViews()
        setupRecyclerViews()
        setupListeners()
        initManagers()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        transferMethodGroup = findViewById(R.id.transferMethodGroup)
        bluetoothRadio = findViewById(R.id.bluetoothRadio)
        wifiRadio = findViewById(R.id.wifiRadio)
        filesRecyclerView = findViewById(R.id.filesRecyclerView)
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        selectFilesButton = findViewById(R.id.selectFilesButton)
        sendButton = findViewById(R.id.sendButton)
        searchProgress = findViewById(R.id.searchProgress)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        fileAdapter = FileAdapter(selectedFiles)
        filesRecyclerView.layoutManager = LinearLayoutManager(this)
        filesRecyclerView.adapter = fileAdapter

        deviceAdapter = DeviceAdapter(discoveredDevices) { device ->
            connectToDevice(device)
        }
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        devicesRecyclerView.adapter = deviceAdapter
    }

    private fun setupListeners() {
        selectFilesButton.setOnClickListener {
            startActivityForResult(
                Intent(this, FilePickerActivity::class.java),
                FILE_PICKER_REQUEST
            )
        }

        sendButton.setOnClickListener {
            sendFiles()
        }

        transferMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.bluetoothRadio -> startBluetoothDiscovery()
                R.id.wifiRadio -> startWiFiDiscovery()
            }
        }
    }

    private fun initManagers() {
        bluetoothManager = BluetoothManager(this)
        wifiDirectManager = WiFiDirectManager(this)
        startBluetoothDiscovery()
    }

    private fun startBluetoothDiscovery() {
        searchProgress.visibility = View.VISIBLE
        discoveredDevices.clear()
        deviceAdapter.notifyDataSetChanged()

        bluetoothManager.startDiscovery { devices ->
            runOnUiThread {
                discoveredDevices.clear()
                discoveredDevices.addAll(devices)
                deviceAdapter.notifyDataSetChanged()
                searchProgress.visibility = View.GONE
            }
        }
    }

    private fun startWiFiDiscovery() {
        searchProgress.visibility = View.VISIBLE
        discoveredDevices.clear()
        deviceAdapter.notifyDataSetChanged()

        wifiDirectManager.discoverPeers { devices ->
            runOnUiThread {
                discoveredDevices.clear()
                discoveredDevices.addAll(devices)
                deviceAdapter.notifyDataSetChanged()
                searchProgress.visibility = View.GONE
            }
        }
    }

    private fun connectToDevice(device: DeviceItem) {
        Toast.makeText(this, "جاري الاتصال بـ ${device.name}", Toast.LENGTH_SHORT).show()
        
        if (bluetoothRadio.isChecked) {
            bluetoothManager.connectToDevice(device) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "تم الاتصال بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "فشل الاتصال", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            wifiDirectManager.connectToDevice(device) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "تم الاتصال بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "فشل الاتصال", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun sendFiles() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "الرجاء اختيار ملفات للإرسال", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "جاري إرسال ${selectedFiles.size} ملف", Toast.LENGTH_SHORT).show()
        
        // تنفيذ عملية الإرسال
        if (bluetoothRadio.isChecked) {
            bluetoothManager.sendFiles(selectedFiles) { progress, success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "تم الإرسال بنجاح", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            wifiDirectManager.sendFiles(selectedFiles) { progress, success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "تم الإرسال بنجاح", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST && resultCode == RESULT_OK) {
            val files = data?.getParcelableArrayListExtra<FileItem>("selected_files")
            files?.let {
                selectedFiles.clear()
                selectedFiles.addAll(it)
                fileAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
        wifiDirectManager.cleanup()
    }
}
