package com.botpa.turbophotos.screens.video

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.ImageView
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
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.GalleryActivity
import com.botpa.turbophotos.gallery.views.ZoomableLayout
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.google.android.material.slider.Slider
import java.io.File
import kotlin.math.max

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

    private var isLooping = true
    private var isSeeking = false
    private var hideControllerOnReady = true

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
    private val showLoading = Runnable { overlayLoading.visibility = View.VISIBLE }

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
    private lateinit var overlayPlay: View
    private lateinit var overlayPlayIcon: ImageView
    private lateinit var overlayLoading: View
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

        //Enable HDR
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        //Init components
        backManager = BackManager(this@VideoActivity, onBackPressedDispatcher)
        initViews()
        initListeners()
        initPlayer()

        //Init activity
        initActivity()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Stop video
        player.stop()
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

        //Views (overlay)
        overlayLayout = findViewById(R.id.overlayLayout)
        overlayName = findViewById(R.id.overlayName)
        overlayPlay = findViewById(R.id.overlayPlay)
        overlayPlayIcon = findViewById(R.id.overlayPlayIcon)
        overlayLoading = findViewById(R.id.overlayLoading)
        overlayTimeSlider = findViewById(R.id.overlayTimeSlider)
        overlayTimeCurrent = findViewById(R.id.overlayTimeCurrent)
        overlayTimeDuration = findViewById(R.id.overlayTimeDuration)

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
    }

    private fun initListeners() {
        //Player
        playerZoom.setOnClick {
            toggleController()
        }

        //Overlay
        overlayPlay.setOnClickListener { view ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

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
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        //Create
        player = ExoPlayer.Builder(this@VideoActivity).build()

        //Init player
        player.playWhenReady = true
        player.addListener(object : Player.Listener {

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)

                //Set is loading
                showLoadingIndicator(isLoading)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    //Ready
                    ExoPlayer.STATE_READY -> {
                        //Stop loading animation
                        showLoadingIndicator(false)

                        //Hide controller
                        if (hideControllerOnReady) {
                            hideControllerOnReady = false
                            showController(false)
                        }
                    }

                    //Ended
                    ExoPlayer.STATE_ENDED -> {
                        //Update player time
                        updatePlayerTime()

                        //Stop loading animation
                        if (!player.isLoading) showLoadingIndicator(false)
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
                    overlayPlayIcon.setImageResource(R.drawable.pause)

                    //Start time update loop
                    if (overlayLayout.isVisible) enableUpdateTimeLoop(true)

                    //Keep screen on
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    //Update play button
                    overlayPlayIcon.setImageResource(R.drawable.play)

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
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

        //Init player view
        playerView.controllerAutoShow = false
        showController(false)

        //Bind player
        playerView.player = player
    }

    //Playback
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
        overlayTimeSlider.valueTo = max(0, player.duration).toFloat()
        overlayTimeSlider.value = player.currentPosition.toFloat()
        overlayTimeCurrent.text = formatMilliseconds(player.currentPosition)
        overlayTimeDuration.text = formatMilliseconds(player.duration)
    }

    private fun showLoadingIndicator(show: Boolean) {
        //Remove loading callbacks
        handler.removeCallbacks(showLoading)

        //Check if loading
        if (show) {
            //Start loading animation
            handler.postDelayed(showLoading, 500)
        } else {
            //Stop loading animation
            overlayLoading.visibility = View.GONE
        }
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
