package com.example.cassette_player

import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var insertSound: MediaPlayer
    private lateinit var playSound: MediaPlayer
    private lateinit var pauseSound: MediaPlayer
    private lateinit var rewindForwardSound: MediaPlayer
    private lateinit var rewindBackwardSound: MediaPlayer

    private lateinit var cassetteAnimation: AnimationDrawable
    private lateinit var cassetteImageView: FrameLayout
    private var isAnimationRunning = false
    private var isRewinding = false
    private var isFastForwarding = false
    private var currentFrame = 0 // Переменная для хранения текущего кадра анимации

    // Настройки скоростей анимации
    private val normalFrameDelay = 30L   // Ускоренная обычная скорость
    private val fastFrameDelay = 15L     // Более быстрая скорость для перемотки

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

        // Инициализация анимации для кассеты
        cassetteImageView = findViewById(R.id.cassette_image)
        cassetteImageView.setBackgroundResource(R.drawable.cassette_animation)
        cassetteAnimation = cassetteImageView.background as AnimationDrawable

        // Настройка событий для кнопок
        findViewById<ImageButton>(R.id.insert_button).setOnClickListener { playSoundOnce(insertSound) }
        findViewById<ImageButton>(R.id.play_button).setOnClickListener {
            playSoundOnce(playSound)
            if (!isAnimationRunning) startCassetteAnimation() // Начать анимацию только если она не запущена
        }
        findViewById<ImageButton>(R.id.pause_button).setOnClickListener {
            playSoundOnce(pauseSound)
            stopCassetteAnimation()
        }

        // Обработка удержания для кнопок перемотки назад и вперёд
        val rewindBackButton = findViewById<ImageButton>(R.id.rewind_back_button)
        rewindBackButton.setOnTouchListener { _, event ->
            handleRewindBackTouchEvent(event, rewindBackwardSound, rewindBackButton)
        }

        val rewindForwardButton = findViewById<ImageButton>(R.id.rewind_forward_button)
        rewindForwardButton.setOnTouchListener { _, event ->
            handleRewindForwardTouchEvent(event, rewindForwardSound, rewindForwardButton)
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

    // Запуск анимации вращения кассеты
    private fun startCassetteAnimation() {
        if (!isAnimationRunning) {
            isAnimationRunning = true
            isRewinding = false
            isFastForwarding = false
            animateFrames(normalFrameDelay) // Начинаем анимацию с обычной скорости
        }
    }

    // Анимация кадров с заданной задержкой
    private fun animateFrames(frameDelay: Long) {
        if (isAnimationRunning && !isRewinding && !isFastForwarding) {
            currentFrame = (currentFrame + 1) % cassetteAnimation.numberOfFrames
            cassetteAnimation.selectDrawable(currentFrame)

            cassetteImageView.postDelayed({
                if (isAnimationRunning && !isRewinding && !isFastForwarding) {
                    animateFrames(frameDelay) // Рекурсивный вызов для следующего кадра
                }
            }, frameDelay)
        }
    }

    // Анимация кадров в обратном порядке с заданной задержкой для перемотки назад
    private fun animateFramesReversed() {
        if (isRewinding) {
            currentFrame = if (currentFrame - 1 < 0) cassetteAnimation.numberOfFrames - 1 else currentFrame - 1
            cassetteAnimation.selectDrawable(currentFrame)

            cassetteImageView.postDelayed({
                if (isRewinding) {
                    animateFramesReversed() // Рекурсивный вызов для предыдущего кадра
                }
            }, fastFrameDelay) // Увеличенная скорость воспроизведения
        }
    }

    // Анимация кадров вперед на повышенной скорости для перемотки вперёд
    private fun animateFramesForward() {
        if (isFastForwarding) {
            currentFrame = (currentFrame + 1) % cassetteAnimation.numberOfFrames
            cassetteAnimation.selectDrawable(currentFrame)

            cassetteImageView.postDelayed({
                if (isFastForwarding) {
                    animateFramesForward() // Рекурсивный вызов для следующего кадра
                }
            }, fastFrameDelay) // Увеличенная скорость воспроизведения
        }
    }

    // Остановка анимации вращения кассеты
    private fun stopCassetteAnimation() {
        isAnimationRunning = false
        isRewinding = false
        isFastForwarding = false
    }

    // Обработка удержания кнопки для перемотки назад
    private fun handleRewindBackTouchEvent(event: MotionEvent, sound: MediaPlayer, button: ImageButton): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                button.alpha = 0.75f // Затемняем кнопку при нажатии
                sound.start()
                sound.isLooping = true // Зацикливаем звук

                if (!isRewinding) {
                    isRewinding = true
                    isAnimationRunning = true
                    animateFramesReversed()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.alpha = 1.0f // Возвращаем кнопку к нормальному состоянию
                sound.pause()
                sound.seekTo(0)
                sound.isLooping = false // Отключаем зацикливание

                if (isRewinding) {
                    isRewinding = false
                    animateFrames(normalFrameDelay)
                }
            }
        }
        return true
    }

    // Обработка удержания кнопки для перемотки вперёд
    private fun handleRewindForwardTouchEvent(event: MotionEvent, sound: MediaPlayer, button: ImageButton): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                button.alpha = 0.75f // Затемняем кнопку при нажатии
                sound.start()
                sound.isLooping = true // Зацикливаем звук

                if (!isFastForwarding) {
                    isFastForwarding = true
                    isAnimationRunning = true
                    animateFramesForward()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.alpha = 1.0f // Возвращаем кнопку к нормальному состоянию
                sound.pause()
                sound.seekTo(0)
                sound.isLooping = false // Отключаем зацикливание

                if (isFastForwarding) {
                    isFastForwarding = false
                    animateFrames(normalFrameDelay)
                }
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
