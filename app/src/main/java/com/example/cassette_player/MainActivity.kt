package com.example.cassette_player

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var insertSound: MediaPlayer
    private lateinit var playSound: MediaPlayer
    private lateinit var pauseSound: MediaPlayer
    private lateinit var rewindForwardSound: MediaPlayer
    private lateinit var rewindBackwardSound: MediaPlayer

    // Для управления длительным нажатием
    private val handler = Handler()
    private var isRewindPressed = false
    private var longPressDuration = 500L // Время для активации длительного нажатия

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Полноэкранный режим
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // Инициализация звуков
        insertSound = MediaPlayer.create(this, R.raw.insert)
        playSound = MediaPlayer.create(this, R.raw.play)
        pauseSound = MediaPlayer.create(this, R.raw.stop)
        rewindForwardSound = MediaPlayer.create(this, R.raw.rewind)
        rewindBackwardSound = MediaPlayer.create(this, R.raw.rewind)

        // Настройка событий для кнопок
        findViewById<ImageButton>(R.id.insert_button).setOnClickListener { playSoundOnce(insertSound) }
        findViewById<ImageButton>(R.id.play_button).setOnClickListener { playSoundOnce(playSound) }
        findViewById<ImageButton>(R.id.pause_button).setOnClickListener { playSoundOnce(pauseSound) }

        // Обработка удержания для кнопок перемотки
        val rewindBackButton = findViewById<ImageButton>(R.id.rewind_back_button)
        val rewindForwardButton = findViewById<ImageButton>(R.id.rewind_forward_button)

        rewindBackButton.setOnTouchListener { _, event ->
            handleRewindTouchEvent(event, rewindBackwardSound, rewindBackButton)
        }

        rewindForwardButton.setOnTouchListener { _, event ->
            handleRewindTouchEvent(event, rewindForwardSound, rewindForwardButton)
        }
    }

    // Проигрывание звука один раз
    private fun playSoundOnce(sound: MediaPlayer) {
        if (sound.isPlaying) {
            sound.pause()
            sound.seekTo(0)
        }
        sound.start()
    }

    // Обработка удержания кнопки для перемотки
    private fun handleRewindTouchEvent(event: MotionEvent, sound: MediaPlayer, button: ImageButton): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Затемняем кнопку при нажатии
                button.alpha = 0.75f // Устанавливаем альфа-канал для затемнения
                isRewindPressed = true

                // Запуск воспроизведения звука после задержки
                handler.postDelayed({
                    if (isRewindPressed) {
                        sound.start()
                        sound.isLooping = true // Начинаем зацикливание звука
                    }
                }, longPressDuration)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Возвращаем цвет кнопки в исходное состояние
                button.alpha = 1.0f // Возвращаем альфа-канал к 1 (нормальное состояние)
                isRewindPressed = false

                // Остановка звука
                sound.isLooping = false
                sound.pause()
                sound.seekTo(0) // Возвращаемся к началу

                // Удаление любых запланированных действий
                handler.removeCallbacksAndMessages(null)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Освобождение ресурсов MediaPlayer
        if (::insertSound.isInitialized) insertSound.release()
        if (::playSound.isInitialized) playSound.release()
        if (::pauseSound.isInitialized) pauseSound.release()
        if (::rewindForwardSound.isInitialized) rewindForwardSound.release()
        if (::rewindBackwardSound.isInitialized) rewindBackwardSound.release()
    }
}
