package com.omarsanjaq.shareapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.omarsanjaq.shareapp.adapters.FileAdapter
import java.io.File

class FilePickerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var allFilesButton: MaterialButton
    private lateinit var imagesButton: MaterialButton
    private lateinit var videosButton: MaterialButton
    private lateinit var appsButton: MaterialButton
    private lateinit var documentsButton: MaterialButton
    private lateinit var musicButton: MaterialButton
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var selectedCountText: TextView
    private lateinit var doneButton: MaterialButton

    private lateinit var fileAdapter: FileAdapter
    private val allFiles = mutableListOf<FileItem>()
    private val selectedFiles = mutableListOf<FileItem>()

    private var currentCategory = FileCategory.ALL

    enum class FileCategory {
        ALL, IMAGES, VIDEOS, APPS, DOCUMENTS, MUSIC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_picker)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadFiles(FileCategory.ALL)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        allFilesButton = findViewById(R.id.allFilesButton)
        imagesButton = findViewById(R.id.imagesButton)
        videosButton = findViewById(R.id.videosButton)
        appsButton = findViewById(R.id.appsButton)
        documentsButton = findViewById(R.id.documentsButton)
        musicButton = findViewById(R.id.musicButton)
        filesRecyclerView = findViewById(R.id.filesRecyclerView)
        selectedCountText = findViewById(R.id.selectedCountText)
        doneButton = findViewById(R.id.doneButton)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(allFiles) { file, isSelected ->
            if (isSelected) {
                selectedFiles.add(file)
            } else {
                selectedFiles.remove(file)
            }
            updateSelectedCount()
        }
        filesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        filesRecyclerView.adapter = fileAdapter
    }

    private fun setupListeners() {
        allFilesButton.setOnClickListener { loadFiles(FileCategory.ALL) }
        imagesButton.setOnClickListener { loadFiles(FileCategory.IMAGES) }
        videosButton.setOnClickListener { loadFiles(FileCategory.VIDEOS) }
        appsButton.setOnClickListener { loadFiles(FileCategory.APPS) }
        documentsButton.setOnClickListener { loadFiles(FileCategory.DOCUMENTS) }
        musicButton.setOnClickListener { loadFiles(FileCategory.MUSIC) }

        doneButton.setOnClickListener {
            val intent = Intent()
            intent.putParcelableArrayListExtra("selected_files", ArrayList(selectedFiles))
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun loadFiles(category: FileCategory) {
        currentCategory = category
        allFiles.clear()
        fileAdapter.notifyDataSetChanged()

        when (category) {
            FileCategory.ALL -> loadAllFiles()
            FileCategory.IMAGES -> loadMediaFiles(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            FileCategory.VIDEOS -> loadMediaFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
            FileCategory.APPS -> loadInstalledApps()
            FileCategory.DOCUMENTS -> loadDocuments()
            FileCategory.MUSIC -> loadMediaFiles(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*")
        }

        fileAdapter.notifyDataSetChanged()
        updateCategoryButtons(category)
    }

    private fun loadAllFiles() {
        loadMediaFiles(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        loadMediaFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
        loadMediaFiles(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*")
        loadDocuments()
    }

    private fun loadMediaFiles(uri: Uri, mimeType: String) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA
        )

        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val path = cursor.getString(dataColumn)

                    val fileItem = FileItem(
                        id = id,
                        name = name,
                        path = path,
                        size = size,
                        mimeType = mimeType,
                        uri = Uri.withAppendedPath(uri, id.toString())
                    )
                    allFiles.add(fileItem)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadInstalledApps() {
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (packageInfo in packages) {
                val appName = packageManager.getApplicationLabel(packageInfo).toString()
                val apkPath = packageInfo.sourceDir
                val apkFile = File(apkPath)

                if (apkFile.exists()) {
                    val fileItem = FileItem(
                        id = packageInfo.uid.toLong(),
                        name = "$appName.apk",
                        path = apkPath,
                        size = apkFile.length(),
                        mimeType = "application/vnd.android.package-archive",
                        uri = Uri.fromFile(apkFile)
                    )
                    allFiles.add(fileItem)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadDocuments() {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        loadFilesFromDirectory(documentsDir)
        loadFilesFromDirectory(downloadDir)
    }

    private fun loadFilesFromDirectory(directory: File?) {
        directory?.listFiles()?.forEach { file ->
            if (file.isFile) {
                val fileItem = FileItem(
                    id = file.hashCode().toLong(),
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    mimeType = getMimeType(file.name),
                    uri = Uri.fromFile(file)
                )
                allFiles.add(fileItem)
            } else if (file.isDirectory) {
                loadFilesFromDirectory(file)
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".doc") || fileName.endsWith(".docx") -> "application/msword"
            fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> "application/vnd.ms-excel"
            fileName.endsWith(".ppt") || fileName.endsWith(".pptx") -> "application/vnd.ms-powerpoint"
            fileName.endsWith(".txt") -> "text/plain"
            fileName.endsWith(".zip") -> "application/zip"
            fileName.endsWith(".rar") -> "application/x-rar-compressed"
            else -> "application/octet-stream"
        }
    }

    private fun updateSelectedCount() {
        selectedCountText.text = "${selectedFiles.size} ملفات محددة"
    }

    private fun updateCategoryButtons(category: FileCategory) {
        allFilesButton.isSelected = category == FileCategory.ALL
        imagesButton.isSelected = category == FileCategory.IMAGES
        videosButton.isSelected = category == FileCategory.VIDEOS
        appsButton.isSelected = category == FileCategory.APPS
        documentsButton.isSelected = category == FileCategory.DOCUMENTS
        musicButton.isSelected = category == FileCategory.MUSIC
    }
}
