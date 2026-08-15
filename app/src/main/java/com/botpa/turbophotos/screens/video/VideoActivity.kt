@file:OptIn(UnstableApi::class)

package com.botpa.turbophotos.screens.video

import android.Manifest
import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.BaseActivity
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.modals.SliderDialog
import com.botpa.turbophotos.gallery.options.OptionsGroup
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.options.OptionsManager
import com.botpa.turbophotos.gallery.permissions.PermissionType
import com.botpa.turbophotos.screens.video.tracks.TracksDialog
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@SuppressLint("SetTextI18n", "NotifyDataSetChanged", "DefaultLocale")
class VideoActivity : BaseActivity() {

     /*$    /$$ /$$       /$$
    | $$   | $$|__/      | $$
    | $$   | $$ /$$  /$$$$$$$  /$$$$$$   /$$$$$$
    |  $$ / $$/| $$ /$$__  $$ /$$__  $$ /$$__  $$
     \  $$ $$/ | $$| $$  | $$| $$$$$$$$| $$  \ $$
      \  $$$/  | $$| $$  | $$| $$_____/| $$  | $$
       \  $/   | $$|  $$$$$$$|  $$$$$$$|  $$$$$$/
        \_/    |__/ \_______/ \_______/ \_____*/

    //Activity
    override val permissions: List<PermissionType> = listOf(PermissionType.Media, PermissionType.Notifications)
    override val contentViewResource: Int = R.layout.video_screen

    private val handler = Handler(Looper.getMainLooper())

    private var isInit = false

    //Permissions
    private val requestPermissionMedia = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted: Map<String, Boolean> ->
        permissionManager.notifyPermissionChanged(PermissionType.Media)
        checkPermissions()
    }
    private val requestPermissionNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        permissionManager.notifyPermissionChanged(PermissionType.Notifications)
        checkPermissions()
    }

    //Player
    private lateinit var player: ExoPlayer

    private var isLooping: Boolean = true
    private var isSeeking: Boolean = false
    private var isInPiP: Boolean = false

    private var automaticPiP = true

    private var ignoreAudioFocus = true
    private var hasAudioFocus = false
    private var resumeOnAudioFocusGain = false

    private var mediaTitle: String = ""

    private val updateTimeLoop: Runnable = object : Runnable {
        override fun run() {
            //Update time
            updatePlayerTime()

            //Loop
            handler.postDelayed(this, 200)
        }
    }

    private lateinit var playerZoom: VideoZoomableLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerSubtitles: SubtitleView

    private val playerAudioTracks = mutableListOf<MediaTrackInfo>()
    private val playerSubtitleTracks = mutableListOf<MediaTrackInfo>()

    //Notification
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification

    private var isNotificationInit: Boolean = false

    //Indicators (loading & time skip)
    private val showLoadingIndicator = Runnable {
        loadingIndicator.visibility = View.VISIBLE
    }
    private val hideSkipIndicators = Runnable {
        Orion.animateHide(skipBackwardsIndicator)
        Orion.animateHide(skipForwardIndicator)
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

    private val options: MutableList<OptionsGroup> = ArrayList()
    private lateinit var optionsManager: OptionsManager

    private lateinit var optionPiP: OptionsItem
    private lateinit var optionSpeed: OptionsItem
    private lateinit var optionSubtitles: OptionsItem
    private lateinit var optionAudio: OptionsItem

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
    private lateinit var overlayTitle: TextView
    private lateinit var overlayLoop: MaterialButton
    private lateinit var overlayPlay: MaterialButton
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
    override fun onBeforeInitViews() {
        //Animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        }

        //Background is always black so we use dark theme status bar
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false

        //Init options
        optionsManager = OptionsManager(this, options, backManager) { onUpdateOptions() }
    }

    override fun onInitViews() {
        //Player
        playerZoom = findViewById(R.id.playerZoom)
        playerView = findViewById(R.id.playerView)
        playerSubtitles = findViewById(R.id.playerSubtitles)

        //Indicators
        loadingIndicator = findViewById(R.id.loadingIndicator)
        skipBackwardsIndicator = findViewById(R.id.skipBackwardsIndicator)
        skipForwardIndicator = findViewById(R.id.skipForwardIndicator)

        //Overlay
        overlayLayout = findViewById(R.id.overlayLayout)
        overlayTitle = findViewById(R.id.overlayTitle)
        overlayLoop = findViewById(R.id.overlayLoop)
        overlayPlay = findViewById(R.id.overlayPlay)
        overlayOptions = findViewById(R.id.overlayOptions)
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

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsManager.layout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )
    }

    override fun onInitListeners() {
        //Player
        playerZoom.setOnSingleClickListener {
            //Toggle controller on single click
            toggleController()
        }

        playerZoom.onBeforeSeek = { amount ->
            //Skip time -> Check direction
            handler.removeCallbacks(hideSkipIndicators)
            if (amount < 0) {
                //Update indicators
                skipBackwardsIndicator.text = "${amount}s"
                Orion.animateShow(skipBackwardsIndicator)
                Orion.animateHide(skipForwardIndicator)
            } else {
                //Update indicators
                skipForwardIndicator.text = "+${amount}s"
                Orion.animateHide(skipBackwardsIndicator)
                Orion.animateShow(skipForwardIndicator)
            }
        }

        playerZoom.onSeek = { amount ->
            //Seek player
            val newPosition = (player.currentPosition + (amount * 1000L)).coerceAtLeast(0).coerceAtMost(player.duration)
            player.seekTo(newPosition)
            overlayTimeSlider.value = newPosition.toFloat()

            //Hide indicators
            handler.postDelayed(hideSkipIndicators, 1000)
        }

        //Overlay
        overlayLoop.setOnClickListener { setLooping(!isLooping) }

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

        overlayOptions.setOnClickListener { optionsManager.toggle(true) }

        overlayTimeSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {

            override fun onStartTrackingTouch(slider: Slider) {
                //Start seeking
                isSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                //Check if seeking manually
                if (isSeeking) player.seekTo(overlayTimeSlider.value.toLong())

                //Update time
                overlayTimeCurrent.text = formatMilliseconds(overlayTimeSlider.value.toLong())

                //Stop seeking
                isSeeking = false
            }

        })

        //Options
        optionPiP = OptionsItem(R.drawable.pip, R.string.context_option_pip) {
            //Enter PiP
            isInPiP = enterPictureInPictureMode(getParamsForPiP())
        }

        optionSpeed = OptionsItem(R.drawable.speed, R.string.video_option_speed) {
            //Create speed slider dialog
            SliderDialog(this@VideoActivity, R.string.video_option_speed, player.playbackParameters.speed, 0.25f, 2f, 0.25f) { speed ->
                //Update speed
                player.setPlaybackSpeed(speed)
            }.buildAndShow()
        }

        optionSubtitles = OptionsItem(R.drawable.track_subtitles, R.string.video_option_subtitles) {
            //Create subtitles track dialog
            TracksDialog(this@VideoActivity, playerSubtitleTracks, getString(R.string.video_option_subtitles)) { track ->
                //Check track
                if (track.trackIndex < 0) {
                    //Disable
                    disableSubtitles()
                } else {
                    //Select track
                    selectTrack(track)
                }
            }.buildAndShow()
        }

        optionAudio = OptionsItem(R.drawable.track_audio, R.string.video_option_audio) {
            //Create audio track dialog
            TracksDialog(this@VideoActivity, playerAudioTracks, getString(R.string.video_option_audio)) { track ->
                //Check track
                if (track.trackIndex < 0) {
                    //Disable
                    disableAudio()
                } else {
                    //Select track
                    selectTrack(track)
                }
            }.buildAndShow()
        }
    }

    override fun onAfterInitViews() {
        //Hide player & controller (ignore versions that have a bug where the video doesn't load the first frame if invisible)
        if (Build.VERSION.SDK_INT !in Build.VERSION_CODES.S..Build.VERSION_CODES.TIRAMISU) {
            playerView.visibility = View.INVISIBLE
        }
        overlayLayout.visibility = View.GONE

        //Init components
        initPlayer()
        initMediaSession()
        initNotification()
        initBroadcastReceiver()
    }

    override fun onRequestPermission(permission: PermissionType) {
        when (permission) {
            //Media
            PermissionType.Media -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionMedia.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
                } else {
                    requestPermissionMedia.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
            //Notifications
            PermissionType.Notifications -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            //Other
            else -> {}
        }
    }

    override fun onPermissionsGranted() {
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

    override fun onDestroy() {
        super.onDestroy()

        //Stop video
        player.stop()
        player.release()

        //Release media session
        mediaSession.release()

        //Cancel notification
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onPause() {
        super.onPause()

        //Pause video
        if (!isInPiP && !automaticPiP && player.isPlaying) {
            player.pause()
        }
    }

    override fun onResume() {
        super.onResume()

        //Not init
        if (!isInit) return

        //Check for permissions
        if (!permissionManager.hasAllPermissions) {
            checkPermissions()
            return
        }

        //Update settings
        playerZoom.skipBackwardsAmount = Storage.getLong(StoragePairs.VIDEO_SKIP_BACKWARDS)
        playerZoom.skipForwardAmount = Storage.getLong(StoragePairs.VIDEO_SKIP_FORWARD)
        ignoreAudioFocus = Storage.getBool(StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS)
        automaticPiP = Storage.getBool(StoragePairs.VIDEO_AUTOMATIC_PIP)
        updateParamsForPiP()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        //Handle intent
        handleIntent(intent)
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
    private fun initPlayer() {
        //Create player
        player = ExoPlayer.Builder(this).build()

        //Init player config
        player.playWhenReady = true
        setLooping(Storage.getBool(StoragePairs.VIDEO_LOOP))

        //Init player listeners
        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                //Prevents the first frame of the video from taking up the whole screen instead of its size
                playerView.visibility = View.VISIBLE
            }

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
                    overlayPlay.text = getString(R.string.video_state_pause)

                    //Start time update loop
                    if (overlayLayout.isVisible) enableUpdateTimeLoop(true)

                    //Keep screen on
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    //Update play button
                    overlayPlay.setIconResource(R.drawable.play)
                    overlayPlay.text = getString(R.string.video_state_play)

                    //Stop time update loop
                    enableUpdateTimeLoop(false)

                    //Disable keeping screen on
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)

                //Update PiP information
                updateParamsForPiP()
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                //Show error
                Orion.snack(this@VideoActivity, error.toString())
            }

            override fun onTracksChanged(tracks: Tracks) {
                //Clear tracks
                playerSubtitleTracks.clear()
                playerAudioTracks.clear()

                //Check track groups
                for (group in tracks.groups) {
                    //Get track type
                    val trackType = group.type

                    //Check if it's audio or subtitles
                    if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            //Ignore if not supported
                            if (!group.isTrackSupported(i)) continue

                            //Get format
                            val format = group.getTrackFormat(i)

                            //Create a user-friendly display name
                            val langCode = format.language
                            val readableLanguage = if (langCode == "und")
                                getString(R.string.video_state_track_default)
                            else
                                langCode?.let { Locale.forLanguageTag(it).displayLanguage }
                            val label = format.label ?: readableLanguage ?: getString(R.string.video_state_track, i + 1)

                            //Create track
                            val trackInfo = MediaTrackInfo(
                                name = label,
                                language = langCode,
                                trackGroup = group,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i)
                            )

                            //Add track to its list
                            if (trackType == C.TRACK_TYPE_AUDIO) {
                                playerAudioTracks.add(trackInfo)
                            } else {
                                playerSubtitleTracks.add(trackInfo)
                            }
                        }
                    }
                }

                //Add "disabled" tracks
                playerSubtitleTracks.add(0, MediaTrackInfo(getString(R.string.video_state_track_disabled), isSelected = !playerSubtitleTracks.any { it.isSelected }))
                playerAudioTracks.add(0, MediaTrackInfo(getString(R.string.video_state_track_disabled), isSelected = !playerAudioTracks.any { it.isSelected }))
            }

            override fun onCues(cueGroup: CueGroup) {
                //Pass the cues to the subtitles view
                playerSubtitles.setCues(cueGroup.cues)
            }
        })

        //Init player view
        playerView.player = player
        playerView.subtitleView?.visibility = View.GONE
        playerSubtitles.setApplyEmbeddedFontSizes(false)
        playerSubtitles.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        toggleController(Storage.getBool(StoragePairs.VIDEO_SHOW_CONTROLS_ON_START))
    }

    private fun initMediaSession() {
        //Create forwarding player
        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    COMMAND_SEEK_TO_NEXT,
                    COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    COMMAND_SEEK_TO_PREVIOUS,
                    COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> false
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .remove(COMMAND_SEEK_TO_NEXT)
                    .remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .remove(COMMAND_SEEK_TO_PREVIOUS)
                    .remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }
        }

        //Create media session
        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .build()
    }

    private fun initNotification() {
        //Get notification manager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //Create notification channel
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        channel.description = getString(R.string.video_notification_channel_description)
        notificationManager.createNotificationChannel(channel)

        //Create intents
        val resumeIntent = Intent(this, VideoActivity::class.java)
        val pauseIntent = Intent(NOTIFICATION_BROADCAST_ID).putExtra("command", "play/pause")

        //Create style
        val style = MediaStyleNotificationHelper.MediaStyle(mediaSession)
            .setShowActionsInCompactView(0)

        //Create notification
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(PendingIntent.getActivity(this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .setSilent(true)
            .setStyle(style)
            .addAction(NotificationCompat.Action(
                if (player.isPlaying) R.drawable.pause else R.drawable.play,
                getString(if (player.isPlaying) R.string.video_state_pause else R.string.video_state_play),
                PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            ))
        notification = builder.build()

        //Show notification
        notificationManager.notify(NOTIFICATION_ID, notification)

        //Mark as init
        isNotificationInit = true
    }

    private fun initBroadcastReceiver() {
        //Create broadcast receiver
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //Get command
                val command = intent.getStringExtra("command") ?: ""

                //Play & pause
                if (command == "play/pause") overlayPlay.performClick()
            }
        }

        //Register broadcast receiver
        ContextCompat.registerReceiver(this, receiver, IntentFilter(NOTIFICATION_BROADCAST_ID), ContextCompat.RECEIVER_NOT_EXPORTED)
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
        mediaTitle = name
        overlayTitle.text = name

        //Create media item
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(name)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()

        //Play media
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun toggleController(show: Boolean) {
        if (show) {
            //Show
            Orion.animateShow(overlayLayout)
            Orion.toggleSystemUI(this, true)

            //Start update time loop
            if (player.isPlaying) enableUpdateTimeLoop(true)
        } else {
            //Hide
            Orion.animateHide(overlayLayout)
            Orion.toggleSystemUI(this, false)

            //Stop update time loop
            enableUpdateTimeLoop(false)
        }
    }

    private fun toggleController() {
        toggleController(!overlayLayout.isVisible)
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
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_GAIN) hasAudioFocus = true
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

    private fun selectTrack(trackInfo: MediaTrackInfo) {
        //Ignore if missing group
        if (trackInfo.trackGroup == null) return

        //Select track
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(trackInfo.trackGroup.mediaTrackGroup, trackInfo.trackIndex))
            .setTrackTypeDisabled(trackInfo.trackGroup.type, false)
            .build()
    }

    private fun disableSubtitles() {
        //Disable text track
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    private fun disableAudio() {
        //Disable audio track
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
    }

    //Playback
    override fun onPictureInPictureUiStateChanged(pipState: android.app.PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)

        //Hide controller before entering PiP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && pipState.isTransitioningToPip) {
            toggleController(false)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        //Update state
        isInPiP = isInPictureInPictureMode

        //Check state
        if (!isInPiP && lifecycle.currentState == Lifecycle.State.CREATED) {
            //PiP was destroyed -> Destroy activity
            finish()
        } else {
            //PiP was opened/closed -> Toggle controller
            toggleController(!isInPiP)
            updatePlayerTime()
        }
    }

    private fun getParamsForPiP(): PictureInPictureParams {
        //Get info
        val sourceRect = Rect()
        playerView.getGlobalVisibleRect(sourceRect)
        val size = player.videoSize

        //Get PiP params
        return PictureInPictureParams.Builder()
            .setAspectRatio(
                if (size.width > 0 && size.height > 0)
                    Rational(size.width, size.height)
                else
                    Rational(16, 9)
            )
            .setSourceRectHint(sourceRect)
            .setAutoEnterEnabled(automaticPiP)
            .build()
    }

    private fun updateParamsForPiP() {
        //App is closing
        if (isFinishing || isDestroyed) return

        //Update PiP params
        setPictureInPictureParams(getParamsForPiP())
    }

    private fun setLooping(looping: Boolean) {
        //Set looping
        isLooping = looping
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        Storage.putBool(StoragePairs.VIDEO_LOOP, isLooping)

        //Update loop button
        overlayLoop.setIconResource(if (isLooping) R.drawable.repeat_on else R.drawable.repeat)
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

    private fun onUpdateOptions() {
        options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
            add(optionPiP)
            add(optionSpeed)
            if (playerSubtitleTracks.size > 1) {
                add(optionSubtitles)
            }
            if (playerAudioTracks.size > 1) {
                add(optionAudio)
            }
        }))
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
    private fun formatMilliseconds(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    //Static
    companion object {

        //Notifications
        private const val NOTIFICATION_ID = 111
        private const val NOTIFICATION_CHANNEL_ID = "video_player"
        private const val NOTIFICATION_CHANNEL_NAME = "Video player"
        private const val NOTIFICATION_BROADCAST_ID = "video_player_notification_broadcast"

    }

}
