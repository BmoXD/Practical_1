package com.example.practical_1

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotosFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val imageUris = getMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        recyclerView.adapter = GalleryAdapter(imageUris)

        return view
    }

    private fun getMedia(contentUri: Uri): List<Uri> {
        val mediaList = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor: Cursor? = requireContext().contentResolver.query(contentUri, projection, null, null, null)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = Uri.withAppendedPath(contentUri, id.toString())
                mediaList.add(uri)
            }
        }
        return mediaList
    }
}
