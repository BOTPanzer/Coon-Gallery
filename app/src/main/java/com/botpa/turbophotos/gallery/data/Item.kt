package com.botpa.turbophotos.gallery.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Orion.compareToAlphanumeric
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import java.io.File

class Item(
    var file: File,               //The file in storage
    var album: Album,             //The album of the item
    var lastModified: Long,       //Timestamp of the last time the file was modified (in seconds)
    var mimeType: String,         //The type and format of the file
    var size: Long,               //The size of the file in bytes
    var isTrashed: Boolean,       //If the file is in the trash
    var isFavourite: Boolean,     //If the file is favourite
) {

    //Item info
    var name: String = if (isTrashed) parseNameFromTrash(file.name) else file.name
    var isVideo: Boolean = mimeType.startsWith("video/") || name.contains(".MP.")


    //Metadata
    fun hasMetadata(): Boolean {
        return album.hasMetadataOfKey(name)
    }

    fun getMetadata(): ItemMetadataInfo? {
        return album.getMetadataOfKey(name)
    }

    //Helpers
    fun updateLastModified(): Boolean {
        //Check if current item was modified
        val newLastModified: Long = file.lastModified() / 1000 //To seconds
        if (newLastModified != lastModified) {
            //Was updated
            lastModified = newLastModified
            return true
        } else {
            //Wasn't updated
            return false
        }
    }

    //Static helpers
    companion object {

        //Comparators
        private val COMPARATOR_DATE_ASCENDING = Comparator<Item> { i1, i2 -> i1.lastModified.compareTo(i2.lastModified) }
        private val COMPARATOR_DATE_DESCENDING = Comparator<Item> { i1, i2 -> i2.lastModified.compareTo(i1.lastModified) }
        private val COMPARATOR_NAME_ASCENDING = Comparator<Item> { i1, i2 -> i1.name.compareToAlphanumeric(i2.name) }
        private val COMPARATOR_NAME_DESCENDING = Comparator<Item> { i1, i2 -> i2.name.compareToAlphanumeric(i1.name) }
        private val COMPARATOR_SIZE_ASCENDING = Comparator<Item> { i1, i2 -> i1.size.compareTo(i2.size) }
        private val COMPARATOR_SIZE_DESCENDING = Comparator<Item> { i1, i2 -> i2.size.compareTo(i1.size) }

        fun getComparator(sortMethod: SortMethod, sortDirection: SortDirection): Comparator<Item> {
            when (sortMethod) {
                //Date
                SortMethod.Date -> {
                    return when (sortDirection) {
                        SortDirection.Ascending -> COMPARATOR_DATE_ASCENDING
                        SortDirection.Descending -> COMPARATOR_DATE_DESCENDING
                    }
                }
                //Name
                SortMethod.Name -> {
                    return when (sortDirection) {
                        SortDirection.Ascending -> COMPARATOR_NAME_ASCENDING
                        SortDirection.Descending -> COMPARATOR_NAME_DESCENDING
                    }
                }
                //Size
                SortMethod.Size -> {
                    return when (sortDirection) {
                        SortDirection.Ascending -> COMPARATOR_SIZE_ASCENDING
                        SortDirection.Descending -> COMPARATOR_SIZE_DESCENDING
                    }
                }
            }
        }

        //Load item preview into ImageView
        fun load(context: Context, imageView: ImageView, item: Item) {
            //Reset image scale type (due to a bug HDR does not load, but idk why changing scale type fixes it)
            imageView.scaleType = ImageView.ScaleType.CENTER

            //Create request
            val request = Glide.with(context)
                .load(item.file.absolutePath)
                .signature(ObjectKey(item.lastModified)) //Reread from disk if cache was updated
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        //Update scale type
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        return false
                    }

                })

            //Load item
            request.into(imageView)
        }

        //Name
        private val trashedNameRegex = Regex("""^\.trashed-\d+-(.+)$""")

        fun parseNameFromTrash(name: String): String {
            return trashedNameRegex.matchEntire(name)?.groupValues?.get(1) ?: name;
        }

        //Factory
        fun createFromFile(file: File, album: Album): Item {
            //Prepare item info
            val lastModified = file.lastModified() / 1000 //To seconds
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Orion.getExtension(file.absolutePath)) ?: "*/*"
            val size = file.length()

            //Create item
            return Item(file, album, lastModified, mimeType, size, isTrashed = false, isFavourite = false)
        }

        fun createFromUri(context: Context, uri: Uri, album: Album): Item {
            //Prepare item info
            val path = Orion.getFilePathFromMediaUri(context, uri) ?: throw Exception("Failed to get path from media URI")
            val file = File(path)

            //Create item
            return createFromFile(file, album)
        }

    }

}
