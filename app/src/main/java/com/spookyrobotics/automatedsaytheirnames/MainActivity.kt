package com.spookyrobotics.automatedsaytheirnames

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.ImageView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.spookyrobotics.automatedsaytheirnames.room.DatabaseWrapper
import com.spookyrobotics.automatedsaytheirnames.room.OfflineContentDao
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants
import com.spookyrobotics.automatedsaytheirnames.room.PoliceVictimEntry
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val utteranceProgressListener: UtteranceProgressListener = buildProgressListener()

    private fun buildProgressListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
            }

            override fun onDone(utteranceId: String) {
                noteRemembered(utteranceId)
            }

            override fun onError(utteranceId: String) {
                println("Error speaking: $utteranceId")
            }

        }
    }

    private val PAUSE_VIEW_INDEX: Int = 1
    private val PLAY_VIEW_INDEX: Int = 0

    private var isPlaying: Boolean = false
    private var processingComplete: Boolean = false
    private var textToSpeechInit: Boolean = false
    private lateinit var parseResources: ParseResources
    private val victims: MutableList<PoliceVictimEntry> = mutableListOf()
    private var startDisposable: Disposable? = null
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var mainScope: CoroutineScope
    private lateinit var ioScope: CoroutineScope
    private lateinit var database: OfflineContentDao
    private lateinit var playPauseButton: ViewFlipper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupPlayPauseButton()
        database = provideDatabase()
        mainScope = provideMainCoroutineScope()
        ioScope = provideIoCoroutineScope()
        textToSpeech = TextToSpeech(this, this)
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener)
        parseResources = ParseResources(this, database, ioScope)
        parseResources.main()

    }

    private fun setupPlayPauseButton() {
        playPauseButton = findViewById(R.id.playPauseButton)
        val playView = ImageView(this).apply {
            setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        }
        val pauseView = ImageView(this).apply {
            setImageResource(R.drawable.ic_baseline_stop_24)
        }
        playPauseButton.addView(playView, PLAY_VIEW_INDEX)
        playPauseButton.addView(pauseView, PAUSE_VIEW_INDEX)
        playPauseButton.displayedChild = PLAY_VIEW_INDEX
        playPauseButton.setOnClickListener {
            if (playPauseButton.displayedChild == PLAY_VIEW_INDEX) {
                startPlaybackIfPossible()
            } else {
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        if (isPlaying) {
            mainScope.launch {
                textToSpeech.playSilentUtterance(3000, TextToSpeech.QUEUE_FLUSH, "ending")
                playPauseButton.displayedChild = PLAY_VIEW_INDEX
                isPlaying = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startDisposable = parseResources.onProcessingComplete().subscribe {
            if (it == true) {
                mainScope.launch {
                    setVictims(database.getAll())
                    processingComplete = true
                }
            }
        }
    }

    @Synchronized
    private fun noteRemembered(utteranceId: String) {
        val rememberedVictimIndex = victims.indexOfFirst { it.utteranceId == utteranceId }
        victims.removeAt(rememberedVictimIndex)
    }

    private fun startPlaybackIfPossible() {
        if (processingComplete && textToSpeechInit && !isPlaying) {
            mainScope.launch {
                isPlaying = true
                playPauseButton.displayedChild = PAUSE_VIEW_INDEX
                victims.forEach {
                    speakTheirName(it)
                }
            }
        }
    }

    @Synchronized
    private fun setVictims(victimList: List<PoliceVictimEntry>) {
        victims.clear()
        victims.addAll(victimList)
    }

    private fun speakTheirName(victim: PoliceVictimEntry) {
        textToSpeech.speak(victim.name, TextToSpeech.QUEUE_ADD, null)
        textToSpeech.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, victim.utteranceId)
    }

    fun provideDatabase(): OfflineContentDao {
        return Room.databaseBuilder(
            application,
            DatabaseWrapper::class.java,
            OfflineDatabaseConstants.DATABASE_FILENAME
        )
            .build()
            .contentDao()
    }

    fun provideMainCoroutineScope(): CoroutineScope {
        return object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.Main
        }
    }

    fun provideIoCoroutineScope(): CoroutineScope {
        return object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.IO
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            val result = textToSpeech . setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                println("Failed to set locale to us")
            } else {
                textToSpeechInit = true
                startPlaybackIfPossible()
            }

        } else {
            println("failed to init")
        }
    }

    override fun onDestroy() {
// Don't forget to shutdown tts!
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
