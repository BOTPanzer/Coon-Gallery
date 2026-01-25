package com.botpa.turbophotos.screens.video

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.GalleryActivity
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.options.OptionsAdapter
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.views.ZoomableLayout
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import java.io.File
import kotlin.math.max
import kotlin.math.min

@SuppressLint("SetTextI18n", "NotifyDataSetChanged", "DefaultLocale")
class VideoActivity : GalleryActivity() {

     /*$    /$$ /$$       /$$
    | $$   | $$|__/      | $$
    | $$   | $$ /$$  /$$$$$$$  /$$$$$$   /$$$$$$
    |  $$ / $$/| $$ /$$__  $$ /$$__  $$ /$$__  $$
     \  $$ $$/ | $$| $$  | $$| $$$$$$$$| $$  \ $$
      \  $$$/  | $$| $$  | $$| $$_____/| $$  | $$
       \  $/   | $$|  $$$$$$$|  $$$$$$$|  $$$$$$/
        \_/    |__/ \_______/ \_______/ \_____*/

    //Activity
    private lateinit var backManager: BackManager
    private var isInit = false

    //Player
    private lateinit var player: ExoPlayer

    private var hideControllerOnReady: Boolean = true
    private var isLooping: Boolean = true
    private var isSeeking: Boolean = false
    private var isInPiP: Boolean = false
    private var skipBackwardsAmount: Long = 5
    private var skipForwardAmount: Long = 5

    private lateinit var playerZoom: ZoomableLayout
    private lateinit var playerView: PlayerView

    //Audio focus
    private var ignoreAudioFocus = true
    private var hasAudioFocus = false
    private var resumeOnAudioFocusGain = false

    //Time update loop & show loading
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeLoop: Runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 200)
            updatePlayerTime()
        }
    }

    //Indicators (loading & time skip)
    private var lastSkipDuration: Long = 0

    private val showLoadingIndicator = Runnable {
        loadingIndicator.visibility = View.VISIBLE
    }
    private val hideSkipIndicators = Runnable {
        Orion.hideAnim(skipBackwardsIndicator)
        Orion.hideAnim(skipForwardIndicator)
        lastSkipDuration = 0
    }

    private lateinit var loadingIndicator: View
    private lateinit var skipBackwardsIndicator: TextView
    private lateinit var skipForwardIndicator: TextView

      /*$$$$$              /$$     /$$
     /$$__  $$            | $$    |__/
    | $$  \ $$  /$$$$$$  /$$$$$$   /$$  /$$$$$$  /$$$$$$$   /$$$$$$$
    | $$  | $$ /$$__  $$|_  $$_/  | $$ /$$__  $$| $$__  $$ /$$_____/
    | $$  | $$| $$  \ $$  | $$    | $$| $$  \ $$| $$  \ $$|  $$$$$$
    | $$  | $$| $$  | $$  | $$ /$$| $$| $$  | $$| $$  | $$ \____  $$
    |  $$$$$$/| $$$$$$$/  |  $$$$/| $$|  $$$$$$/| $$  | $$ /$$$$$$$/
     \______/ | $$____/    \___/  |__/ \______/ |__/  |__/|_______/
              | $$
              | $$
              |_*/

    private val options: MutableList<OptionsItem> = ArrayList()
    private lateinit var optionsAdapter: OptionsAdapter

    private val optionSeparator: OptionsItem = OptionsItem()
    private lateinit var optionPiP: OptionsItem

    private lateinit var optionsLayout: View
    private lateinit var optionsList: RecyclerView

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //Views (overlay)
    private lateinit var overlayLayout: View
    private lateinit var overlayName: TextView
    private lateinit var overlayPlay: MaterialButton
    private lateinit var overlayLoop: MaterialButton
    private lateinit var overlayOptions: MaterialButton
    private lateinit var overlayTimeSlider: Slider
    private lateinit var overlayTimeCurrent: TextView
    private lateinit var overlayTimeDuration: TextView



     /*$    /$$ /$$       /$$
    | $$   | $$|__/      | $$
    | $$   | $$ /$$  /$$$$$$$  /$$$$$$   /$$$$$$
    |  $$ / $$/| $$ /$$__  $$ /$$__  $$ /$$__  $$
     \  $$ $$/ | $$| $$  | $$| $$$$$$$$| $$  \ $$
      \  $$$/  | $$| $$  | $$| $$_____/| $$  | $$
       \  $/   | $$|  $$$$$$$|  $$$$$$$|  $$$$$$/
        \_/    |__/ \_______/ \_______/ \_____*/

    //Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.video_screen)

        //Background is always black so we use dark theme status bar
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false

        //Enable HDR
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        //Init components
        Storage.init(this) //Init storage cause activity is exported
        backManager = BackManager(this, onBackPressedDispatcher)
        initViews()
        initListeners()
        initLists()
        initPlayer()

        //Init activity
        initActivity()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Stop video
        player.stop()
    }

    override fun onPause() {
        super.onPause()

        //Pause video
        if (!isInPiP) player.pause()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        //Handle intent
        handleIntent(intent)
    }

    private fun initActivity() {
        //Check if intent is valid
        val intent = getIntent()
        if (intent == null) {
            finish()
            return
        }

        //Handle intent
        handleIntent(intent)

        //Mark as init
        isInit = true
    }

    private fun handleIntent(intent: Intent) {
        //Get intent data
        val uri: Uri? = intent.data
        if (uri == null) {
            finish()
            return
        }

        //Play uri
        playMedia(uri, getNameFromUri(uri))
    }

    //Components
    private fun initViews() {
        //Views (player)
        playerZoom = findViewById(R.id.playerZoom)
        playerView = findViewById(R.id.playerView)

        //Views (indicators)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        skipBackwardsIndicator = findViewById(R.id.skipBackwardsIndicator)
        skipForwardIndicator = findViewById(R.id.skipForwardIndicator)

        //Views (overlay)
        overlayLayout = findViewById(R.id.overlayLayout)
        overlayName = findViewById(R.id.overlayName)
        overlayPlay = findViewById(R.id.overlayPlay)
        overlayLoop = findViewById(R.id.overlayLoop)
        overlayOptions = findViewById(R.id.overlayOptions)
        overlayTimeSlider = findViewById(R.id.overlayTimeSlider)
        overlayTimeCurrent = findViewById(R.id.overlayTimeCurrent)
        overlayTimeDuration = findViewById(R.id.overlayTimeDuration)

        //Views (options)
        optionsLayout = findViewById(R.id.optionsLayout)
        optionsList = findViewById(R.id.optionsList)


        //Insets (overlay)
        Orion.addInsetsChangedListener(
            findViewById(R.id.overlayIndent),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, percent: Float ->
            //Ignore if no margins
            if (insets.top <= 0 && insets.bottom <= 0) return@addInsetsChangedListener

            //Update margins
            val params = view.layoutParams as MarginLayoutParams
            params.setMargins(insets.left, insets.top, insets.right, insets.bottom)
            view.layoutParams = params
        }

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsLayout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )
    }

    private fun initListeners() {
        //Player
        playerZoom.onClick = {
            toggleController()
        }

        playerZoom.onDoubleClick = { x, y ->
            //Get layout width
            val width = playerZoom.width
            val doubleTapArea = width / 5

            //Check position to see if should skip time
            if (x <= doubleTapArea || x >= width - doubleTapArea) {
                //Skip time -> Check direction
                if (x < doubleTapArea) {
                    //Skip backwards
                    val newPosition = (player.currentPosition - (skipBackwardsAmount * 1000L)).coerceAtLeast(0)
                    player.seekTo(newPosition)
                    overlayTimeSlider.value = newPosition.toFloat()

                    //Update indicators
                    lastSkipDuration = min(lastSkipDuration - skipBackwardsAmount, -skipBackwardsAmount)
                    skipBackwardsIndicator.text = "${lastSkipDuration}s"
                    Orion.showAnim(skipBackwardsIndicator)
                    Orion.hideAnim(skipForwardIndicator)
                } else if (x > width - doubleTapArea) {
                    //Skip forward
                    val newPosition = (player.currentPosition + (skipForwardAmount * 1000L)).coerceAtMost(player.duration)
                    player.seekTo(newPosition)
                    overlayTimeSlider.value = newPosition.toFloat()

                    //Update indicators
                    lastSkipDuration = max(lastSkipDuration + skipForwardAmount, skipForwardAmount)
                    skipForwardIndicator.text = "+${lastSkipDuration}s"
                    Orion.hideAnim(skipBackwardsIndicator)
                    Orion.showAnim(skipForwardIndicator)
                }

                //Seeking
                handler.removeCallbacks(hideSkipIndicators)
                handler.postDelayed(hideSkipIndicators, 1000)

                //Consume click
                true
            } else {
                //Don't skip time -> Don't consume click
                false
            }
        }

        //Overlay
        overlayPlay.setOnClickListener { view ->
            if (player.playbackState == ExoPlayer.STATE_ENDED) {
                //Ended -> Restart
                player.seekTo(0)
            } else if (player.isPlaying) {
                //Playing -> Pause
                player.pause()
            } else {
                //Not playing -> Play
                player.play()
            }
        }

        overlayLoop.setOnClickListener { setLooping(!isLooping) }

        overlayOptions.setOnClickListener { toggleOptions(true) }

        overlayTimeSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {

            override fun onStartTrackingTouch(slider: Slider) {
                //Start seeking
                isSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                //Check if seeking manually
                if (isSeeking) player.seekTo(overlayTimeSlider.value.toLong())

                //Stop seeking
                isSeeking = false
            }

        })

        //Options
        optionsLayout.setOnClickListener { toggleOptions(false) }

        optionPiP = OptionsItem(R.drawable.pip, "Open in PiP") {
            //Create params
            val p = PictureInPictureParams.Builder()
            try {
                val size = player.videoSize
                if (size.width <= 0 || size.height <= 0) throw Exception()
                p.setAspectRatio(Rational(size.width, size.height))
            } catch (_: Exception) {
                p.setAspectRatio(Rational(16, 9))
            }

            //Enter PiP
            isInPiP = enterPictureInPictureMode(p.build())
        }
    }

    private fun initLists() {
        //Init options layout manager
        optionsList.setLayoutManager(LinearLayoutManager(this@VideoActivity))

        //Init options adapter
        optionsAdapter = OptionsAdapter(this@VideoActivity, options)
        optionsAdapter.setOnClickListener { view: View, index: Int ->
            //Get option
            val option = options[index]

            //Get action
            val action = option.action ?: return@setOnClickListener

            //Invoke action
            action.run()
            toggleOptions(false)
        }
        optionsList.setAdapter(optionsAdapter)
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        //Create
        player = ExoPlayer.Builder(this@VideoActivity).build()

        //Init player
        setLooping(Storage.getBool(StoragePairs.VIDEO_LOOP))
        skipBackwardsAmount = Storage.getLong(StoragePairs.VIDEO_SKIP_BACKWARDS)
        skipForwardAmount = Storage.getLong(StoragePairs.VIDEO_SKIP_FORWARD)
        player.playWhenReady = true
        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    //Media is buffering
                    ExoPlayer.STATE_BUFFERING -> {
                        //Show loading animation
                        showLoadingIndicator(true)
                    }

                    //Media is ready
                    ExoPlayer.STATE_READY -> {
                        //Hide loading animation
                        showLoadingIndicator(false)

                        //Hide controller
                        if (hideControllerOnReady) {
                            hideControllerOnReady = false
                            showController(false)
                        }
                    }

                    //Finished playing media
                    ExoPlayer.STATE_ENDED -> {
                        //Update player time
                        updatePlayerTime()
                    }

                    //Other
                    else -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    //Request audio focus
                    requestAudioFocus()

                    //Update play button
                    overlayPlay.setIconResource(R.drawable.pause)
                    overlayPlay.text = "Pause"

                    //Start time update loop
                    if (overlayLayout.isVisible) enableUpdateTimeLoop(true)

                    //Keep screen on
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    //Update play button
                    overlayPlay.setIconResource(R.drawable.play)
                    overlayPlay.text = "Play"

                    //Stop time update loop
                    enableUpdateTimeLoop(false)

                    //Disable keeping screen on
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                //Show error
                Orion.snack(this@VideoActivity, error.toString())
            }

        })

        //Init player view
        playerView.controllerAutoShow = false
        showController(false)

        //Bind player
        playerView.player = player
    }

    //Player
    private fun getNameFromUri(uri: Uri): String {
        //Get path from uri
        var path = uri.path!!

        //Check if uri is a URL
        if (path.startsWith("http://") || path.startsWith("https://")) return "URL video"

        //Fix external files path
        if (path.startsWith("/external_files/")) path = path.replaceFirst("/external_files/", Orion.externalStorageDir)

        //Check if path exists
        var file = File(path)
        if (file.exists()) return file.name

        //Get path from uri
        file = File(Orion.getFilePathFromDocumentProviderUri(this, uri) ?: uri.toString())
        if (file.exists()) return file.name

        //Couldn't find name
        return "URI video"
    }

    private fun playMedia(uri: Uri, name: String) {
        //Update title
        overlayName.text = name

        //Play media
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun showController(show: Boolean) {
        if (show) {
            //Show
            Orion.showAnim(overlayLayout)
            toggleSystemUI(true)

            //Start update time loop
            if (player.isPlaying) enableUpdateTimeLoop(true)
        } else {
            //Hide
            Orion.hideAnim(overlayLayout)
            toggleSystemUI(false)

            //Stop update time loop
            enableUpdateTimeLoop(false)
        }
    }

    private fun toggleController() {
        showController(!overlayLayout.isVisible)
    }

    private fun enableUpdateTimeLoop(enable: Boolean) {
        //Stop update time loop
        handler.removeCallbacks(updateTimeLoop)

        //Start update time loop
        if (enable) updateTimeLoop.run()
    }

    private fun updatePlayerTime() {
        //Player is seeking -> Ignore
        if (isSeeking) return

        //Update time
        val duration = max(0, player.duration).toFloat()
        overlayTimeSlider.valueTo = duration
        overlayTimeSlider.value = min(duration, player.currentPosition.toFloat())
        overlayTimeDuration.text = formatMilliseconds(player.duration)
        overlayTimeCurrent.text = formatMilliseconds(player.currentPosition)
    }

    private fun showLoadingIndicator(show: Boolean) {
        //Remove loading callbacks
        handler.removeCallbacks(showLoadingIndicator)

        //Check if loading
        if (show) {
            //Start loading animation
            handler.postDelayed(showLoadingIndicator, 300)
        } else {
            //Stop loading animation
            loadingIndicator.visibility = View.GONE
        }
    }

    //Playback
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        //Update state & toggle controller
        isInPiP = isInPictureInPictureMode
        showController(!isInPictureInPictureMode)
    }

    private fun setLooping(looping: Boolean) {
        //Set looping
        isLooping = looping
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        Storage.putBool(StoragePairs.VIDEO_LOOP.key, isLooping)

        //Update loop button
        overlayLoop.setIconResource(if (isLooping) R.drawable.repeat_on else R.drawable.repeat)
    }

    //Audio focus
    private fun requestAudioFocus() {
        //Ignoring audio focus or its already granted
        if (ignoreAudioFocus || hasAudioFocus) return

        //Get audio manager
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        //Create playback attributes
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

        //Create focus request
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { state ->
                when (state) {
                    //Granted
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        //Update audio focus
                        hasAudioFocus = true

                        //Resume playing
                        if (resumeOnAudioFocusGain && !player.isPlaying) player.play()
                        resumeOnAudioFocusGain = false
                    }

                    //Lost
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        //Update audio focus
                        hasAudioFocus = false

                        //Resume playing later when audio focus is regained
                        resumeOnAudioFocusGain = if (state == AudioManager.AUDIOFOCUS_LOSS) false else player.isPlaying

                        //Pause
                        if (player.isPlaying) player.pause()
                    }
                }
            }
            .setWillPauseWhenDucked(true)
            .build()

        //Request audio focus
        val state = audioManager.requestAudioFocus(focusRequest)
        if (state == AudioManager.AUDIOFOCUS_GAIN) hasAudioFocus = true
    }

      /*$$$$$              /$$     /$$
     /$$__  $$            | $$    |__/
    | $$  \ $$  /$$$$$$  /$$$$$$   /$$  /$$$$$$  /$$$$$$$   /$$$$$$$
    | $$  | $$ /$$__  $$|_  $$_/  | $$ /$$__  $$| $$__  $$ /$$_____/
    | $$  | $$| $$  \ $$  | $$    | $$| $$  \ $$| $$  \ $$|  $$$$$$
    | $$  | $$| $$  | $$  | $$ /$$| $$| $$  | $$| $$  | $$ \____  $$
    |  $$$$$$/| $$$$$$$/  |  $$$$/| $$|  $$$$$$/| $$  | $$ /$$$$$$$/
     \______/ | $$____/    \___/  |__/ \______/ |__/  |__/|_______/
              | $$
              | $$
              |_*/

    private fun toggleOptions(show: Boolean) {
        if (show) {
            //Update options list
            options.clear()
            options.add(optionPiP)
            optionsAdapter.notifyDataSetChanged()

            //Show
            Orion.showAnim(optionsLayout)
            backManager.register("options") { toggleOptions(false) }
        } else {
            //Hide
            Orion.hideAnim(optionsLayout)
            backManager.unregister("options")
        }
    }

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //Util
    private fun toggleSystemUI(show: Boolean) {
        //Get controller
        val controller = WindowCompat.getInsetsController(window, overlayLayout)

        //Toggle system UI
        if (show) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun formatMilliseconds(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

}
