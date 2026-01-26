package com.botpa.turbophotos.util

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.botpa.turbophotos.R
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.isVisible
import androidx.core.view.isGone
import androidx.core.net.toUri
import androidx.core.graphics.scale

object Orion {

    //Logging
    private const val loggingTag = "Orion";

    //Json
    private val objectMapper = ObjectMapper()


    //Math
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a * (1 - t) + b * t
    }

    //Get attribute color
    fun getColor(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    //Snack bar
    @JvmOverloads
    fun snack(activity: Activity, message: String, button: String = "ok", runnable: Runnable? = null) {
        val objLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, "nepe", Snackbar.LENGTH_LONG)

        @SuppressLint("RestrictedApi") val layout = snackbar.getView() as SnackbarLayout
        layout.setPadding(0, 0, 0, 0)
        layout.setBackgroundColor(0x00000000)

        val inflater = (LayoutInflater.from(activity))
        val snackView = inflater.inflate(R.layout.snackbar_one, null)

        val snackText = snackView.findViewById<TextView>(R.id.textView)
        snackText.text = message

        val textViewOne = snackView.findViewById<TextView>(R.id.txtOne)
        textViewOne.text = button.uppercase(Locale.getDefault())
        textViewOne.setOnClickListener { view: View ->
            runnable?.run()
            snackbar.dismiss()
        }

        layout.addView(snackView, objLayoutParams)
        snackbar.show()
    }

    fun snackTwo(activity: Activity, message: String, cancel: String, confirm: String, runnable: Runnable) {
        val objLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, "nepe", Snackbar.LENGTH_LONG)

        @SuppressLint("RestrictedApi") val layout = snackbar.getView() as SnackbarLayout
        layout.setPadding(0, 0, 0, 0)
        layout.setBackgroundColor(0x00000000)

        val inflater = (LayoutInflater.from(activity))
        val snackView = inflater.inflate(R.layout.snackbar_two, null)

        val snackText = snackView.findViewById<TextView>(R.id.textView)
        snackText.text = message

        val textViewOne = snackView.findViewById<TextView>(R.id.txtOne)
        textViewOne.text = cancel.uppercase(Locale.getDefault())
        textViewOne.setOnClickListener { view: View -> snackbar.dismiss() }

        val textViewTwo = snackView.findViewById<TextView>(R.id.txtTwo)
        textViewTwo.text = confirm.uppercase(Locale.getDefault())
        textViewTwo.setOnClickListener { view: View ->
            runnable.run()
            snackbar.dismiss()
        }

        layout.addView(snackView, objLayoutParams)
        snackbar.show()
    }

    @JvmOverloads
    fun snackTwo(activity: Activity, message: String, button1: String, runnable1: Runnable, button2: String, runnable2: Runnable, length: Int = Snackbar.LENGTH_LONG) {
        val objLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, "nepe", length)

        @SuppressLint("RestrictedApi") val layout = snackbar.getView() as SnackbarLayout
        layout.setPadding(0, 0, 0, 0)
        layout.setBackgroundColor(0x00000000)

        val inflater = (LayoutInflater.from(activity))
        val snackView = inflater.inflate(R.layout.snackbar_two, null)

        val snackText = snackView.findViewById<TextView>(R.id.textView)
        snackText.text = message

        val textViewOne = snackView.findViewById<TextView>(R.id.txtOne)
        textViewOne.text = button1.uppercase(Locale.getDefault())
        textViewOne.setOnClickListener { view: View ->
            runnable1.run()
            snackbar.dismiss()
        }

        val textViewTwo = snackView.findViewById<TextView>(R.id.txtTwo)
        textViewTwo.text = button2.uppercase(Locale.getDefault())
        textViewTwo.setOnClickListener { view: View ->
            runnable2.run()
            snackbar.dismiss()
        }

        layout.addView(snackView, objLayoutParams)
        snackbar.show()
    }

    //Keyboard
    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity.applicationContext)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity.applicationContext)
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    //Focus
    fun clearFocus(activity: Activity) {
        activity.currentFocus?.clearFocus()
    }

    //Animations
    @JvmOverloads
    fun hideAnim(view: View, duration: Int = 150, runnable: Runnable? = null) {
        //Already gone
        if (view.isGone) return

        //Already being animated
        if (view.animation != null && !view.animation.hasEnded()) return

        //Animate
        val alpha = AlphaAnimation(1f, 0f)
        alpha.duration = duration.toLong()
        view.startAnimation(alpha)
        alpha.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                view.visibility = View.GONE
                runnable?.run()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    fun hideAnim(view: View, runnable: Runnable?) {
        hideAnim(view, 150, runnable)
    }

    @JvmOverloads
    fun showAnim(view: View, duration: Int = 150, runnable: Runnable? = null) {
        //Already visible
        if (view.isVisible) return

        //Animate
        val alpha = AlphaAnimation(0f, 1f)
        alpha.duration = duration.toLong()
        view.startAnimation(alpha)
        view.visibility = View.VISIBLE
        runnable?.run()
    }

    fun showAnim(view: View, runnable: Runnable?) {
        showAnim(view, 150, runnable)
    }

    class ResizeWidthAnimation : Animation {

        private val view: View
        private val width: Int
        private val startWidth: Int

        constructor(view: View, width: Int) {
            this.view = view
            this.width = width
            startWidth = view.width
        }

        constructor(view: View, width: Int, startWidth: Int) {
            this.view = view
            this.width = width
            this.startWidth = startWidth
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            view.layoutParams.width = startWidth + ((width - startWidth) * interpolatedTime).toInt()
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }

    }

    class ResizeHeightAnimation : Animation {

        private val view: View
        private val height: Int
        private val startHeight: Int

        constructor(view: View, height: Int) {
            this.view = view
            this.height = height
            startHeight = view.height
        }

        constructor(view: View, height: Int, startHeight: Int) {
            this.view = view
            this.height = height
            this.startHeight = startHeight
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            view.layoutParams.height = startHeight + ((height - startHeight) * interpolatedTime).toInt()
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }

    }

    //Insets
    fun interface OnInsetsChanged {
        fun run(view: View, insets: Insets, percent: Float)
    }

    val onInsetsChangedDefault: OnInsetsChanged = OnInsetsChanged { view: View, insets: Insets, duration: Float ->
        view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    @JvmOverloads
    fun addInsetsChangedListener(view: View, types: IntArray, duration: Float = 0f, onInsetsChanged: OnInsetsChanged = onInsetsChangedDefault) {
        //No types
        if (types.isEmpty()) return

        //Add listener
        ViewCompat.setOnApplyWindowInsetsListener(view) { view: View, windowInsets: WindowInsetsCompat ->
            //Get insets
            var insets = Insets.of(0, 0, 0, 0)
            for (type in types) insets = Insets.add(insets, windowInsets.getInsets(type)) //WindowInsetsCompat.Type.systemBars()

            //Check if should animate
            if (duration <= 0) {
                //No animation -> Run on insets changed as if it finished
                onInsetsChanged.run(view, insets, 1.0f)
            } else {
                //Calculate start & end values
                val leftStart = view.paddingLeft.toFloat()
                val leftEnd = insets.left.toFloat()
                val topStart = view.paddingTop.toFloat()
                val topEnd = insets.top.toFloat()
                val rightStart = view.paddingRight.toFloat()
                val rightEnd = insets.right.toFloat()
                val botStart = view.paddingBottom.toFloat()
                val botEnd = insets.bottom.toFloat()

                //Animate
                val animator = ValueAnimator.ofFloat(0f, 1f)
                animator.duration = duration.toLong()
                animator.addUpdateListener { animation: ValueAnimator ->
                    //Calculate animation percent
                    val percent = animation.animatedFraction

                    //Run on insets changed
                    onInsetsChanged.run(
                        view,
                        Insets.of(
                            lerp(leftStart, leftEnd, percent).toInt(),
                            lerp(topStart, topEnd, percent).toInt(),
                            lerp(rightStart, rightEnd, percent).toInt(),
                            lerp(botStart, botEnd, percent).toInt()
                        ),
                        percent
                    )
                }
                animator.start()
            }
            windowInsets
        }
    }

    fun addInsetsChangedListener(view: View, types: IntArray, onInsetsChanged: OnInsetsChanged) {
        addInsetsChangedListener(view, types, 0f, onInsetsChanged)
    }

    //Clipboard
    fun copyToClip(context: Context, string: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", string)
        clipboard.setPrimaryClip(clip)
    }

    //Colors
    fun lightenColor(color: Int, fraction: Double): Int {
        var red = Color.red(color)
        var green = Color.green(color)
        var blue = Color.blue(color)
        red = lightenColorValue(red, fraction)
        green = lightenColorValue(green, fraction)
        blue = lightenColorValue(blue, fraction)
        val alpha = Color.alpha(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun darkenColor(color: Int, fraction: Double): Int {
        var red = Color.red(color)
        var green = Color.green(color)
        var blue = Color.blue(color)
        red = darkenColorValue(red, fraction)
        green = darkenColorValue(green, fraction)
        blue = darkenColorValue(blue, fraction)
        val alpha = Color.alpha(color)
        return Color.argb(alpha, red, green, blue)
    }

    private fun darkenColorValue(color: Int, fraction: Double): Int {
        return max(color - (color * fraction), 0.0).toInt()
    }

    private fun lightenColorValue(color: Int, fraction: Double): Int {
        return min(color + (color * fraction), 255.0).toInt()
    }

    //Files
    fun listFolders(parent: File): MutableList<File> {
        //Create folders list
        val folders: MutableList<File> = ArrayList()

        //Get temp items array
        val temp = parent.listFiles() ?: return folders

        //Add folders to list
        for (file in temp) if (file.isDirectory) folders.add(file)
        return folders
    }

    fun listFiles(parent: File): MutableList<File> {
        //Create files list
        val files: MutableList<File> = ArrayList()

        //Get temp items array
        val temp = parent.listFiles() ?: return files

        //Add files to list
        for (file in temp) if (file.isFile) files.add(file)
        return files
    }

    fun listFilesAndFolders(parent: File): MutableList<File> {
        //Create files list
        val files: MutableList<File> = ArrayList()

        //Get temp items array
        val temp = parent.listFiles() ?: return files

        //Add files to list
        files.addAll(listOf<File>(*temp))
        return files
    }

    private fun detectCharset(path: Path): Charset? {
        var charset = StandardCharsets.UTF_8
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(path.toFile())
            val bytes = ByteArray(4096) // Read a chunk of bytes
            val bytesRead = fis.read(bytes)
            charset = if (bytesRead == -1) StandardCharsets.UTF_8 else detectCharsetFromBytes(bytes, bytesRead)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return charset
    }

    private fun detectCharsetFromBytes(bytes: ByteArray, length: Int): Charset? {
        // Simple BOM detection (UTF-8, UTF-16, UTF-32)
        if (length >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8 // UTF-8 BOM
        } else if (length >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE // UTF-16BE BOM
        } else if (length >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE // UTF-16LE BOM
        } /* else if (length >= 4 && bytes[0] == (byte) 0x00 && bytes[1] == (byte) 0x00 && bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF) {
            return "UTF-32BE"; // UTF-32BE BOM
        } else if (length >= 4 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE && bytes[2] == (byte) 0x00 && bytes[3] == (byte) 0x00) {
            return "UTF-32LE"; // UTF-32LE BOM
        }*/

        return StandardCharsets.UTF_8 //default if unable to detect.
    }

    fun getExtension(path: String): String {
        val dotIndex = path.lastIndexOf(".")
        return if (dotIndex == -1) "" else path.substring(dotIndex + 1)
    }

    fun readFile(file: File): String {
        val path = file.toPath()
        val charset = detectCharset(path)
        var reader: Reader? = null

        val sb = StringBuilder()

        try {
            reader = BufferedReader(InputStreamReader(Files.newInputStream(path), charset))
            val buff = CharArray(1024)
            var length: Int
            while ((reader.read(buff).also { length = it }) > 0) {
                sb.append(String(buff, 0, length))
            }
        } catch (e: IOException) {
            val message = e.message
            if (message != null) Log.e(loggingTag, "Failed to read file: $message")
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                val message = e.message
                if (message != null) Log.e(loggingTag, "Failed to read file: $message")
            }
        }

        return sb.toString()
    }

    fun writeFile(file: File, data: String): Boolean {
        var writer: Writer? = null
        var success = true

        try {
            writer = BufferedWriter(OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))
            writer.write(data)
        } catch (e: IOException) {
            val message = e.message
            if (message != null) Log.e(loggingTag, "Failed to write file: $message")
            success = false
        } finally {
            try {
                writer?.close()
            } catch (e: Exception) {
                val message = e.message
                if (message != null) Log.e(loggingTag, "Failed to write file: $message")
            }
        }

        return success
    }

    fun emptyFolder(directory: File, deleteSelf: Boolean): Boolean {
        //Invalid folder
        if (!directory.exists() || !directory.isDirectory) return false

        //List folder files
        val files = directory.listFiles() ?: return true
        if (files.size == 0) return true

        //Empty folder
        var allDeleted = true
        for (file in files) {
            if (file.isDirectory) {
                //Delete folder
                if (!emptyFolder(file, true)) allDeleted = false
            } else {
                //Delete file
                if (!file.delete()) allDeleted = false
            }
        }

        //Delete self
        if (deleteSelf && !directory.delete()) allDeleted = false

        //Return success
        return allDeleted
    }

    fun cloneFile(context: Context, sourceFile: File, destFile: File): Boolean {
        try {
            //Get parent
            val parent = destFile.parentFile ?: return false

            //Create parent folder
            if (!parent.exists() && !parent.mkdirs()) {
                Log.e(loggingTag, "Failed to create folder: $parent.absolutePath")
                return false
            }

            //Copy file
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            scanFile(context, destFile)
            return true
        } catch (_: IOException) {
            return false
        }
    }

    fun scanFile(context: Context?, file: File) {
        MediaScannerConnection.scanFile(context, arrayOf<String>(file.absolutePath), null, null)
    }

    //Files: Json
    val emptyJson: ObjectNode get() = objectMapper.createObjectNode()

    val emptyJsonArray: ArrayNode get() = objectMapper.createArrayNode()

    fun writeJson(file: File, json: ObjectNode): Boolean {
        return writeFile(file, json.toString())
    }

    fun writeJsonPretty(file: File, json: ObjectNode): Boolean {
        return writeFile(file, json.toPrettyString())
    }

    fun loadJson(file: File): ObjectNode {
        //Check if file exists and has content before trying to parse
        if (!file.exists() || file.length() == 0L) return objectMapper.createObjectNode()

        //Parse value
        return try {
            //Get root node
            val rootNode = objectMapper.readTree(file)

            //Check if it's a valid json object
            if (rootNode != null && rootNode.isObject) {
                //Cast and return
                rootNode as ObjectNode
            } else {
                //Not an object -> Return empty
                objectMapper.createObjectNode()
            }
        } catch (_: IOException) {
            //Return empty on error
            objectMapper.createObjectNode()
        }
    }

    fun loadJson(json: String): ObjectNode {
        //Basic check for null or empty string
        if (json.trim { it <= ' ' }.isEmpty()) return objectMapper.createObjectNode()

        //Parse value
        return try {
            //Get root node
            val rootNode = objectMapper.readTree(json)

            //Check if it's a valid json object
            if (rootNode != null && rootNode.isObject()) {
                //Cast and return
                rootNode as ObjectNode
            } else {
                //Not an object -> Return empty
                objectMapper.createObjectNode()
            }
        } catch (_: JsonProcessingException) {
            //Return empty on error
            objectMapper.createObjectNode()
        }
    }

    fun arrayToJson(array: Array<String>): ArrayNode {
        val jsonArray = objectMapper.createArrayNode()
        for (obj in array) jsonArray.add(obj)
        return jsonArray
    }

    //Files: directories
    val externalStorageDir: String get() = "${Environment.getExternalStorageDirectory().absolutePath}/"

    //Files: URIs
    fun getFileUriFromFilePath(context: Context, filePath: String): Uri? {
        //Build query args
        val queryArgs = Bundle()
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Files.FileColumns.DATA} =? ")
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf<String?>(filePath))
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)

        //Create cursor
        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                arrayOf(MediaStore.Files.FileColumns._ID),
                queryArgs,
                null
            ).use { cursor ->
                //Search
                if (cursor != null && cursor.moveToFirst()) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return Uri.withAppendedPath(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), id.toString())
                }
            }
        } catch (e: Exception) {
            //Error
            Log.e(loggingTag, "Failed to get file uri from file path: ${e.message}")
        }

        //Failed
        return null
    }

    fun getMediaUriFromFilePath(context: Context, filePath: String): Uri? {
        //Ger mime type
        val extension = getExtension(filePath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

        //Determine if the file is an image or video to select the correct table
        val contentUri: Uri
        if (mimeType.startsWith("video")) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        //Build query args
        val queryArgs = Bundle()
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Files.FileColumns.DATA} =? ")
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(filePath))
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)

        //Create cursor
        try {
            context.contentResolver.query(
                contentUri,
                arrayOf(MediaStore.MediaColumns._ID),
                queryArgs,
                null
            ).use { cursor ->
                //Search
                if (cursor != null && cursor.moveToFirst()) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return Uri.withAppendedPath(contentUri, id.toString())
                }
            }
        } catch (e: Exception) {
            //Error
            Log.e(loggingTag, "Failed to media get uri from file path: ${e.message}")
        }

        //Failed
        return null
    }

    fun getFilePathFromMediaUri(context: Context, uri: Uri): String? {
        //Build query args
        val queryArgs = Bundle()
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)

        //Data column (file path)
        val column = MediaStore.MediaColumns.DATA

        //Create cursor
        try {
            context.contentResolver.query(
                uri,
                arrayOf(column),
                queryArgs,
                null
            ).use { cursor ->
                //Search
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            //Error
            Log.e(loggingTag, "Failed to get file path from media uri: ${e.message}")
        }

        //Failed
        return null
    }

    fun getFilePathFromDocumentProviderUri(context: Context, uri: Uri): String? {
        //Path
        var path: String? = null

        //IDK, I copied this lol
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                //External storage document
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String> = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    path = "${externalStorageDir}${split[1]}"
                }
            } else if (isDownloadsDocument(uri)) {
                //Downloads document
                val id = DocumentsContract.getDocumentId(uri)

                if (!TextUtils.isEmpty(id) && id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }

                if (id.startsWith("msf:")) {
                    val split: Array<String> = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])

                    path = getDataColumn(
                        context,
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        selection,
                        selectionArgs
                    )
                } else {
                    val contentUri = ContentUris.withAppendedId("content://downloads/public_downloads".toUri(), id.toLong())

                    path = getDataColumn(context, contentUri, null, null)
                }
            } else if (isMediaDocument(uri)) {
                //Media document
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String> = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "${MediaStore.Audio.Media._ID} =?"
                val selectionArgs = arrayOf(split[1])

                path = getDataColumn(context, contentUri!!, selection, selectionArgs)
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
            path = getDataColumn(context, uri, null, null)
        } else if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            path = uri.path
        }

        //Decode path
        if (path != null) {
            return try {
                URLDecoder.decode(path, "UTF-8")
            } catch (_: Exception) {
                null
            }
        }

        //Didn't work
        path = uri.path
        if (path!!.contains(":")) return externalStorageDir + path.substring(path.indexOf(":") + 1)

        //All failed
        return null
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null

        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    //Files: bitmaps
    private fun saveBitmap(dest: String, bitmap: Bitmap) {
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(dest)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getScaledBitmap(path: String, max: Int): Bitmap {
        val src = BitmapFactory.decodeFile(path)

        var width = src.width
        var height = src.height
        var rate: Float

        if (width > height) {
            rate = max / width.toFloat()
            height = (height * rate).toInt()
            width = max
        } else {
            rate = max / height.toFloat()
            width = (width * rate).toInt()
            height = max
        }

        return src.scale(width, height)
    }

    fun decodeSampleBitmapFromPath(path: String?, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        //Calculate in sample size
        var inSampleSize = 1
        val width = options.outWidth
        val height = options.outHeight
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        options.inSampleSize = inSampleSize

        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

}
