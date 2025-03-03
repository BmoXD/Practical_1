package com.example.practical_1

import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class AudioFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AudioAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var audioList = mutableListOf<Uri>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_audio_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadAudioFiles()
        adapter = AudioAdapter(audioList, ::playAudio, ::deleteAudio)
        recyclerView.adapter = adapter

        return view
    }

    private fun loadAudioFiles() {
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Music/Recordings%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                audioList.add(uri)
            }
        }
    }

    private fun playAudio(audioUri: Uri) {
        Firebase.analytics.logEvent("audio_play_button_clicked", null)
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(requireContext(), audioUri)
                prepare()
                start()
                Toast.makeText(requireContext(), "Playing audio", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Playback failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAudio(audioUri: Uri) {
        Firebase.analytics.logEvent("singular_audio_delete_button_clicked", null)
        requireContext().contentResolver.delete(audioUri, null, null)
        audioList.remove(audioUri)
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "Audio deleted", Toast.LENGTH_SHORT).show()
    }
}
