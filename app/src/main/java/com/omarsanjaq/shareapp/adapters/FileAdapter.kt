package com.omarsanjaq.shareapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.omarsanjaq.shareapp.FileItem
import com.omarsanjaq.shareapp.R

class FileAdapter(
    private val files: List<FileItem>,
    private val onFileSelected: ((FileItem, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileCheckbox: CheckBox = view.findViewById(R.id.fileCheckbox)
        val fileIcon: ImageView = view.findViewById(R.id.fileIcon)
        val fileName: TextView = view.findViewById(R.id.fileName)
        val fileSize: TextView = view.findViewById(R.id.fileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        holder.fileName.text = file.name
        holder.fileSize.text = formatFileSize(file.size)

        // Set icon based on file type
        val iconRes = when {
            file.mimeType.startsWith("image/") -> R.drawable.ic_file
            file.mimeType.startsWith("video/") -> R.drawable.ic_file
            file.mimeType.startsWith("audio/") -> R.drawable.ic_file
            else -> R.drawable.ic_file
        }
        holder.fileIcon.setImageResource(iconRes)

        if (onFileSelected != null) {
            holder.fileCheckbox.visibility = View.VISIBLE
            holder.fileCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onFileSelected.invoke(file, isChecked)
            }
        } else {
            holder.fileCheckbox.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            holder.fileCheckbox.isChecked = !holder.fileCheckbox.isChecked
        }
    }

    override fun getItemCount() = files.size

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}
