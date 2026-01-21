package com.botpa.turbophotos.gallery

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.botpa.turbophotos.util.Orion
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class CoonItem(
    @JvmField var file: File,               //The file in storage
    @JvmField var album: Album,             //The album of the item
    @JvmField var lastModified: Long,       //Timestamp of the last time the file was modified (in seconds)
    @JvmField var mimeType: String,         //The type and format of the file
    @JvmField var size: Long,               //The size of the file in bytes
    @JvmField var isTrashed: Boolean,       //If the file is in the trash
) : Comparable<CoonItem> {

    //Item info
    @JvmField var name: String = file.name
    @JvmField var isVideo: Boolean = mimeType.startsWith("video/")


    //Metadata
    fun hasMetadata(): Boolean {
        return album.hasMetadataKey(name)
    }

    fun getMetadata(): ObjectNode? {
        return album.getMetadataKey(name)
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

    override fun compareTo(other: CoonItem): Int {
        return lastModified.compareTo(other.lastModified)
    }

    //Static helpers
    companion object {

        //Load item preview into ImageView
        fun load(context: Context, imageView: ImageView, item: CoonItem, highestQuality: Boolean = false) {
            //Reset image scale type (due to a bug HDR does not load, but idk why changing scale type fixes it)
            imageView.scaleType = ImageView.ScaleType.CENTER

            //Create request
            var request = Glide.with(context)
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
            if (highestQuality) {
                request = request
                    .override(Target.SIZE_ORIGINAL) //Force the highest quality possible
            }

            //Load item
            request.into(imageView)
        }

        //Factory
        fun createFromFile(file: File, album: Album): CoonItem {
            //Prepare item info
            val lastModified = file.lastModified() / 1000 //To seconds
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Orion.getExtension(file.absolutePath)) ?: "*/*"
            val size = file.length()

            //Create item
            return CoonItem(file, album, lastModified, mimeType, size, false)
        }

        fun createFromUri(context: Context, uri: Uri, album: Album): CoonItem {
            //Prepare item info
            val path = Orion.getFilePathFromMediaUri(context, uri)
            val file = File(path)

            //Create item
            return createFromFile(file, album)
        }

    }

}
