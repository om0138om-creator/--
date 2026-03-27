package com.omarsanjaq.shareapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.omarsanjaq.shareapp.adapters.FileAdapter

class ReceiveActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var transferMethodGroup: RadioGroup
    private lateinit var bluetoothRadio: RadioButton
    private lateinit var wifiRadio: RadioButton
    private lateinit var statusIcon: ImageView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var receivedFilesRecyclerView: RecyclerView
    private lateinit var startReceivingButton: MaterialButton

    private lateinit var fileAdapter: FileAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var wifiDirectManager: WiFiDirectManager

    private val receivedFiles = mutableListOf<FileItem>()
    private var isReceiving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

        initViews()
        setupRecyclerView()
        setupListeners()
        initManagers()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        transferMethodGroup = findViewById(R.id.transferMethodGroup)
        bluetoothRadio = findViewById(R.id.bluetoothRadio)
        wifiRadio = findViewById(R.id.wifiRadio)
        statusIcon = findViewById(R.id.statusIcon)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        receivedFilesRecyclerView = findViewById(R.id.receivedFilesRecyclerView)
        startReceivingButton = findViewById(R.id.startReceivingButton)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(receivedFiles)
        receivedFilesRecyclerView.layoutManager = LinearLayoutManager(this)
        receivedFilesRecyclerView.adapter = fileAdapter
    }

    private fun setupListeners() {
        startReceivingButton.setOnClickListener {
            if (isReceiving) {
                stopReceiving()
            } else {
                startReceiving()
            }
        }

        transferMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isReceiving) {
                stopReceiving()
            }
        }
    }

    private fun initManagers() {
        bluetoothManager = BluetoothManager(this)
        wifiDirectManager = WiFiDirectManager(this)
    }

    private fun startReceiving() {
        isReceiving = true
        startReceivingButton.text = "إيقاف الاستقبال"
        statusText.text = "في انتظار الاتصال..."

        if (bluetoothRadio.isChecked) {
            bluetoothManager.startReceiving { file, progress ->
                runOnUiThread {
                    if (file != null) {
                        receivedFiles.add(file)
                        fileAdapter.notifyItemInserted(receivedFiles.size - 1)
                        statusText.text = "تم استلام: ${file.name}"
                    }
                    updateProgress(progress)
                }
            }
        } else {
            wifiDirectManager.startReceiving { file, progress ->
                runOnUiThread {
                    if (file != null) {
                        receivedFiles.add(file)
                        fileAdapter.notifyItemInserted(receivedFiles.size - 1)
                        statusText.text = "تم استلام: ${file.name}"
                    }
                    updateProgress(progress)
                }
            }
        }

        Toast.makeText(this, "جاهز للاستقبال", Toast.LENGTH_SHORT).show()
    }

    private fun stopReceiving() {
        isReceiving = false
        startReceivingButton.text = "بدء الاستقبال"
        statusText.text = "تم إيقاف الاستقبال"
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE

        bluetoothManager.stopReceiving()
        wifiDirectManager.stopReceiving()
    }

    private fun updateProgress(progress: Int) {
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressBar.progress = progress
        progressText.text = "$progress%"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiving) {
            stopReceiving()
        }
        bluetoothManager.cleanup()
        wifiDirectManager.cleanup()
    }
}
