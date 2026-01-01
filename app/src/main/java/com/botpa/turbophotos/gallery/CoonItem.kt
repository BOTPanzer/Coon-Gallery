package com.botpa.turbophotos.gallery

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

class CoonItem(
    @JvmField var file: File,               //The file in storage
    @JvmField var album: Album,             //The album of the item
    @JvmField var lastModified: Long,       //Timestamp of the last time the file was modified (in seconds)
    @JvmField var size: Long,               //The size of the file in bytes
    @JvmField var isVideo: Boolean,         //If the item is a video
    @JvmField var trashInfo: TrashInfo?,    //Info about the item in the trash
) : Comparable<CoonItem> {

    //Item info
    @JvmField var name: String = file.name
    @JvmField var mimeType: String = if (isVideo) "video/*" else "image/*"


    //Helpers
    fun hasMetadata(): Boolean {
        return album.hasMetadataKey(name)
    }

    fun hasTrashInfo(): Boolean {
        return trashInfo != null
    }

    override fun compareTo(other: CoonItem): Int {
        return lastModified.compareTo(other.lastModified)
    }

    //Static helpers
    companion object {

        //Load item preview into ImageView
        fun load(context: Context, imageView: ImageView, item: CoonItem) {
            //Reset image scale type (due to a bug HDR does not load, but idk why changing scale type fixes it)
            imageView.scaleType = ImageView.ScaleType.CENTER

            //Load item preview
            val requestBuilder = Glide.with(context).asBitmap().sizeMultiplier(if (item.isVideo) 0.1f else 0.2f)
            Glide.with(context)
                .asBitmap()
                .load(item.file.absolutePath)
                .thumbnail(requestBuilder)
                .transition(BitmapTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        //Update scale type
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        return false
                    }

                })
                .into(imageView)
        }

    }

}
