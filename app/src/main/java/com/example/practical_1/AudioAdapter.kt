package com.example.practical_1

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AudioAdapter(
    private val audioList: List<Uri>,
    private val playCallback: (Uri) -> Unit,
    private val deleteCallback: (Uri) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val playButton: Button = view.findViewById(R.id.btnPlayRecording)
        val deleteButton: Button = view.findViewById(R.id.btnDeleteRecording)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val uri = audioList[position]
        holder.fileName.text = "Recording ${position + 1}"

        holder.playButton.setOnClickListener { playCallback(uri) }
        holder.deleteButton.setOnClickListener { deleteCallback(uri) }
    }

    override fun getItemCount(): Int = audioList.size
}
