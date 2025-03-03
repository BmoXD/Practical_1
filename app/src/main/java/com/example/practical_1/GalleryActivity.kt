package com.example.practical_1

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class GalleryActivity : BaseActivity() {
    override fun shouldShowGalleryOption(): Boolean {
        return false // Hide the Gallery button when inside the gallery
    }
    override fun shouldShowDeleteOption(): Boolean {
        return true // Show the Delete option in the gallery
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        setSupportActionBar(findViewById(R.id.my_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = GalleryPagerAdapter(this)
        viewPager.adapter = adapter

        // Link tabs with ViewPager only two tabs: photos and videos
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Photos"
                else -> "Videos"
            }
        }.attach()
    }

    override fun onDeletePressed() {
        deleteAllMedia()
    }

    private fun deleteAllMedia() {
        val contentResolver = contentResolver

        // Delete images from "Pictures/CameraX-Image"
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageSelection = "${MediaStore.Images.Media.RELATIVE_PATH}=?"
        val imageArgs = arrayOf("Pictures/CameraX-Image/")
        val deletedImages = contentResolver.delete(imageUri, imageSelection, imageArgs)

        // Delete videos from "Movies/CameraX-Video"
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoSelection = "${MediaStore.Video.Media.RELATIVE_PATH}=?"
        val videoArgs = arrayOf("Movies/CameraX-Video/")
        val deletedVideos = contentResolver.delete(videoUri, videoSelection, videoArgs)

        // Show a Toast message based on what was deleted
        when {
            deletedImages > 0 && deletedVideos > 0 ->
                Toast.makeText(this, "Deleted all photos and videos", Toast.LENGTH_SHORT).show()
            deletedImages > 0 ->
                Toast.makeText(this, "Deleted all photos", Toast.LENGTH_SHORT).show()
            deletedVideos > 0 ->
                Toast.makeText(this, "Deleted all videos", Toast.LENGTH_SHORT).show()
            else ->
                Toast.makeText(this, "No media found", Toast.LENGTH_SHORT).show()
        }

        // Refresh the gallery by scanning the directories again
        refreshGallery()
    }

    private fun refreshGallery() {
        // Path to the specific directories
        val imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/CameraX-Image"
        val videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/CameraX-Video"

        // Scan these directories to refresh the gallery
        MediaScannerConnection.scanFile(
            this,
            arrayOf(imagePath, videoPath),
            null,
            MediaScannerConnection.OnScanCompletedListener { path, uri ->
                // Optionally, you can log the result here if you want to track
                Log.d("GalleryRefresh", "Scanned $path: $uri")
            }
        )
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.toolbar_items, menu)
        return true
    }
}
