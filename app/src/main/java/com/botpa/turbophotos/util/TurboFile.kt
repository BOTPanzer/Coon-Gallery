package com.botpa.turbophotos.util

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

class TurboFile(
    @JvmField var file: File,               //The file in storage of this file
    @JvmField var album: Album,             //The album of the file
    @JvmField var lastModified: Long,       //Timestamp of the last time the file was modified
    @JvmField var size: Float,              //The size of the file in bytes
    @JvmField var isVideo: Boolean,         //If the file is a video
    @JvmField var trashInfo: TrashInfo?,    //Info about the file in the trash
) {

    //File info
    var name: String = file.name
    var mimeType: String = if (isVideo) "video/*" else "image/*"


    //Helpers
    fun hasMetadata(): Boolean {
        return album.hasMetadataKey(name)
    }

    fun isTrashed(): Boolean {
        return trashInfo != null
    }

    //Static helpers
    companion object {

        //Load file into ImageView
        fun load(context: Context, imageView: ImageView, turboFile: TurboFile) {
            //Reset image scale type (due to a bug HDR does not load, but changing scale type fixes it idk why)
            imageView.setScaleType(ImageView.ScaleType.CENTER)

            //Load image
            val requestBuilder = Glide.with(context).asBitmap().sizeMultiplier(0.2f)
            Glide.with(context)
                .asBitmap()
                .load(turboFile.file.absolutePath)
                .thumbnail(requestBuilder)
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
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP)
                        return false
                    }

                })
                .into(imageView)
        }

    }

}
