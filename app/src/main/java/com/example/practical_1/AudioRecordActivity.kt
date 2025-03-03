package com.example.practical_1

import RecordingAdapter
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import android.net.Uri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class AudioRecordActivity : BaseActivity() {

    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var btnPlay: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnDeleteAll: Button
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioUri: Uri? = null
    private var recordings = mutableListOf<Uri>()

    override fun shouldShowVoiceMemoOption(): Boolean {
        return false
    }
    override fun shouldShowDeleteOption(): Boolean {
        return true // Show the Delete option in the gallery
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)

        setSupportActionBar(findViewById(R.id.my_toolbar))

//        val toolbar: Toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        btnPlay = findViewById(R.id.btnPlay)
        recyclerView = findViewById(R.id.recordingsRecyclerView)
        //btnDeleteAll = findViewById(R.id.btnDeleteAll)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadRecordings()

        btnStop.isEnabled = false
        btnPlay.isEnabled = false

        btnRecord.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopRecording() }
        btnPlay.setOnClickListener { playRecording() }
        //btnDeleteAll.setOnClickListener { deleteAllRecordings() }
    }

    override fun onDeletePressed() {
        deleteAllRecordings()
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//
//            android.R.id.home -> {
//                finish() // Navigates back to the previous screen
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    private fun deleteAllRecordings() {
        Firebase.analytics.logEvent("audio_delete_all_button_clicked", null)
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Music/Recordings%")

        val query = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                contentResolver.delete(uri, null, null) // Delete each file
            }
        }

        recordings.clear() // Clear the list

        // Reload the recordings to reflect deletion
        loadRecordings()

        Toast.makeText(this, "All recordings deleted", Toast.LENGTH_SHORT).show()
    }

    private fun startRecording() {
        Firebase.analytics.logEvent("audio_record_button_clicked", null)
        if (checkPermissions()) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "recording_${System.currentTimeMillis()}.3gp")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Recordings")
            }

            val audioCollection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val resolver = contentResolver

            audioUri = resolver.insert(audioCollection, contentValues)
            if (audioUri == null) {
                Toast.makeText(this, "Failed to create audio file", Toast.LENGTH_SHORT).show()
                return
            }

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(resolver.openFileDescriptor(audioUri!!, "w")?.fileDescriptor)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                try {
                    prepare()
                    start()
                    Toast.makeText(this@AudioRecordActivity, "Recording started", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(this@AudioRecordActivity, "Recording failed", Toast.LENGTH_SHORT).show()
                }
            }

            btnRecord.isEnabled = false
            btnStop.isEnabled = true
        }
    }


    private fun stopRecording() {
        Firebase.analytics.logEvent("audio_record_stop_button_clicked", null)
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        // Refresh Media Store so the new recording appears
        audioUri?.let {
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            scanIntent.data = it
            sendBroadcast(scanIntent)
        }

        btnRecord.isEnabled = true
        btnStop.isEnabled = false
        btnPlay.isEnabled = true
        Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()

        // Refresh the list of recordings
        loadRecordings()
    }



    private fun playRecording() {
        Firebase.analytics.logEvent("latest_audio_play_button_clicked", null)
        mediaPlayer?.release()
        audioUri?.let {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(applicationContext, it)
                    prepare()
                    start()
                    Toast.makeText(this@AudioRecordActivity, "Playing latest recording", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(this@AudioRecordActivity, "Playback failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadRecordings() {
        recordings.clear()

        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Music/Recordings%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val query = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                recordings.add(uri)
            }
        }

        val adapter = RecordingAdapter(this, recordings.toMutableList(), contentResolver) { deleteAllRecordings() }
        recyclerView.adapter = adapter
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        return if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), 101)
            false
        } else {
            true
        }
    }
}
