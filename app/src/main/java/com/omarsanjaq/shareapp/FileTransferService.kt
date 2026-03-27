package com.omarsanjaq.shareapp

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FileTransferService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle file transfer in background
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup
    }
}
