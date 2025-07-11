package com.botpa.turbophotos.util

import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

class TurboImage(@JvmField var file: File, @JvmField var album: Album, @JvmField var lastModified: Long, @JvmField var mediaType: String) {

    var name: String = file.name
    var isVideo: Boolean = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    var mimeType: String = if (isVideo) "video/*" else "image/*"

    fun hasMetadata(): Boolean {
        return album.hasMetadataKey(name)
    }

    companion object {

        fun load(context: Context, imageView: ImageView, turboImage: TurboImage) {
            //Reset image scale type (due to a bug HDR does not load, but changing scale type fixes it idk why)
            imageView.setScaleType(ImageView.ScaleType.CENTER)

            //Load image
            val requestBuilder = Glide.with(context).asBitmap().sizeMultiplier(0.3f)
            Glide.with(context)
                .asBitmap()
                .load(turboImage.file.absolutePath)
                .thumbnail(requestBuilder)
                .listener(object : RequestListener<Bitmap> { // Use RequestListener<Bitmap> because we used .asBitmap()
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
                        //Update image scale type (if i don't do it here HDR does not work lol)
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP)
                        return false
                    }

                })
                .into(imageView)
        }

    }

}
