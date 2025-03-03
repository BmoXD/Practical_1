package com.example.practical_1

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class GalleryAdapter(private val mediaList: List<Uri>) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = mediaList[position]
        Glide.with(holder.imageView.context).load(uri).into(holder.imageView)

        // Open media file when clicked
        holder.imageView.setOnClickListener {
            Firebase.analytics.logEvent("gallery_item_clicked", null)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")  // Change "image/*" to "video/*" for videos
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            holder.imageView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = mediaList.size
}
