package com.example.practical_1

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.toolbar_items, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val galleryItem = menu.findItem(R.id.item_gallery)
        val voiceMemoItem = menu.findItem(R.id.voice_memo)
        val deleteItem = menu.findItem(R.id.item_delete)

        // Dynamically show or hide the menu items
        galleryItem?.isVisible = shouldShowGalleryOption()
        voiceMemoItem?.isVisible = shouldShowVoiceMemoOption()
        deleteItem?.isVisible = shouldShowDeleteOption()

        return super.onPrepareOptionsMenu(menu)
    }

    // Default: Show all menu items unless overridden
    open fun shouldShowGalleryOption(): Boolean {
        return true
    }

    open fun shouldShowVoiceMemoOption(): Boolean {
        return true
    }

    open fun shouldShowDeleteOption(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.voice_memo -> {
                startActivity(Intent(this, AudioRecordActivity::class.java))
                Firebase.analytics.logEvent("voice_memo_button_clicked", null)
                true
            }
            R.id.item_gallery -> {
                startActivity(Intent(this, GalleryActivity::class.java))
                Firebase.analytics.logEvent("gallery_button_clicked", null)
                true
            }
            R.id.item_delete -> {
                onDeletePressed()
                Firebase.analytics.logEvent("delete_button_clicked", null)
                true
            }
            android.R.id.home -> {
                finish() // Navigates back to the previous screen
                Firebase.analytics.logEvent("back_button_clicked", null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    open fun onDeletePressed() {}
}
