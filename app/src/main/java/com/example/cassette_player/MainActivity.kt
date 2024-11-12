package com.example.cassette_player

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1
    private lateinit var mediaPlayer: MediaPlayer
    private var currentSongPath: String? = null
    private var currentSongTitle: String? = null
    private var isPaused = false  // Логическая переменная, отслеживающая, находится ли песня на паузе
    private var pausePosition = 0 // Переменная для хранения текущей позиции
    private var showedToastForNoSong = false // Флаг для уведомления
    // Настройки скоростей анимации
    private val normalFrameDelay = 30L   // Ускоренная обычная скорость
    private val fastFrameDelay = 15L     // Более быстрая скорость для перемотки

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()
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
        findViewById<ImageButton>(R.id.insert_button).setOnClickListener { playSoundOnce(insertSound)
            openSongSelectionActivity()}

        //Кнопка "Пуск"
        findViewById<ImageButton>(R.id.play_button).setOnClickListener {
            playSoundOnce(playSound)

            if (currentSongPath != null) {
                try {
                    if (isPaused) {
                        mediaPlayer.seekTo(pausePosition)  // Перемещаемся на сохраненную позицию
                        mediaPlayer.start()                // Продолжаем воспроизведение
                        if (!isAnimationRunning) startCassetteAnimation()
                        isPaused = false                   // Сбрасываем флаг паузы
                        pausePosition = 0                  // Сбрасываем позицию паузы
                    } else {
                        playSong(currentSongPath!!)        // Начинаем воспроизведение сначала
                        if (!isAnimationRunning) startCassetteAnimation()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Ошибка воспроизведения песни", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Песня не выбрана", Toast.LENGTH_SHORT).show()
            }
        }

        //Кнопка "Пауза"
        findViewById<ImageButton>(R.id.pause_button).setOnClickListener {
            playSoundOnce(pauseSound)
            stopCassetteAnimation()
            stopSong()
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

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            // Проверяем разрешения
            if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
                requestPermissions(permissions.toTypedArray(), REQUEST_CODE_READ_EXTERNAL_STORAGE)
            }
        } else {
            // Для Android ниже M (6.0), разрешения даются при установке
            // Открытие активности для выбора песни без дополнительного запроса
            openSongSelectionActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, теперь можно переходить к выбору песни
                // Не открываем SongSelectionActivity автоматически, только по кнопке
            } else {
                Toast.makeText(this, "Разрешение на доступ к аудиофайлам не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSongSelectionActivity() {
        val intent = Intent(this, SongSelectionActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_SELECT_SONG)
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
        if (isFastForwarding && mediaPlayer.isPlaying) {
            currentFrame = (currentFrame + 1) % cassetteAnimation.numberOfFrames
            cassetteAnimation.selectDrawable(currentFrame)

            // Перематываем вперёд на 100 миллисекунд
            mediaPlayer.seekTo(mediaPlayer.currentPosition + 100)

            cassetteImageView.postDelayed({
                if (isFastForwarding) {
                    animateFramesForward() // Рекурсивный вызов для продолжения перемотки
                }
            }, fastFrameDelay) // Ускоренная задержка кадров
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
        if (currentSongPath == null) {  // Проверка, выбрана ли песня
            Toast.makeText(this, "Песня не выбрана", Toast.LENGTH_SHORT).show()
            return false  // Прерываем выполнение, если песня не выбрана
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                button.alpha = 0.75f // Затемняем кнопку при нажатии
                sound.start()
                sound.isLooping = true // Зацикливаем звук

                if (!isRewinding) {
                    isRewinding = true
                    isAnimationRunning = true
                    animateFramesReversed() // Начинаем анимацию перемотки назад
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.alpha = 1.0f // Возвращаем кнопку к нормальному состоянию
                sound.pause()
                sound.seekTo(0)
                sound.isLooping = false // Отключаем зацикливание

                if (isRewinding) {
                    isRewinding = false
                    animateFrames(normalFrameDelay) // Возвращаемся к обычной анимации
                }
            }
        }
        return true
    }

    // Обработка удержания кнопки для перемотки вперёд
    private fun handleRewindForwardTouchEvent(event: MotionEvent, sound: MediaPlayer, button: ImageButton): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Сбрасываем флаг при новом нажатии
                showedToastForNoSong = false
                button.alpha = 0.75f // Затемняем кнопку при нажатии

                // Проверка, выбрана ли песня, если нет - не запускаем перемотку
                if (currentSongPath == null) {
                    return true // Не выполняем действий, если песня не выбрана
                }

                sound.start()
                sound.isLooping = true // Зацикливаем звук

                if (!isFastForwarding && ::mediaPlayer.isInitialized && (mediaPlayer.isPlaying || isPaused)) {
                    isFastForwarding = true
                    isAnimationRunning = true
                    animateFramesForward() // Запускаем анимацию перемотки вперед
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.alpha = 1.0f // Возвращаем кнопку к нормальному состоянию
                sound.pause()
                sound.seekTo(0)
                sound.isLooping = false

                // Показываем уведомление, если песня не выбрана и уведомление еще не показано
                if (currentSongPath == null && !showedToastForNoSong) {
                    Toast.makeText(this, "Песня не выбрана", Toast.LENGTH_SHORT).show()
                    showedToastForNoSong = true
                }

                // Если кнопка отпущена и песня выбрана, останавливаем перемотку и продолжаем проигрывание
                if (isFastForwarding && currentSongPath != null) {
                    isFastForwarding = false

                    if (::mediaPlayer.isInitialized && (mediaPlayer.isPlaying || isPaused)) {
                        mediaPlayer.start() // Продолжаем воспроизведение с новой позиции
                        isPaused = false
                    }
                    animateFrames(normalFrameDelay) // Возвращаемся к обычной анимации
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_SONG && resultCode == Activity.RESULT_OK) {
            val songPath = data?.getStringExtra("song_path")
            val songTitle = data?.getStringExtra("song_title")  // Получаем название песни

            if (!songPath.isNullOrEmpty() && !songTitle.isNullOrEmpty()) {
                // Остановить текущую песню и сбросить триггеры
                stopSong()                 // Останавливаем текущую песню, если играет
                isPaused = false           // Сбрасываем флаг паузы
                pausePosition = 0          // Сбрасываем позицию

                currentSongPath = songPath // Устанавливаем новый путь
                updateCassetteLabel(songTitle) // Обновляем название на кассете
            } else {
                Toast.makeText(this, "Путь к файлу или название не найдено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCassetteLabel(songTitle: String) {
        val cassetteLabel = findViewById<TextView>(R.id.cassette_label)
        cassetteLabel.text = songTitle
    }

    companion object {
        private const val REQUEST_CODE_SELECT_SONG = 1
    }

    private fun playSong(songPath: String) {
        try {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.reset() // Сбрасываем MediaPlayer для новой песни
            } else {
                mediaPlayer = MediaPlayer()
            }

            mediaPlayer.setDataSource(songPath)
            mediaPlayer.prepare()
            mediaPlayer.start()

            startCassetteAnimation() // Запускаем анимацию кассеты при воспроизведении
            isPaused = false         // Сбрасываем флаг паузы
            pausePosition = 0        // Обнуляем позицию для новой песни
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при воспроизведении песни", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSong() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                pausePosition = mediaPlayer.currentPosition // Запоминаем текущую позицию
                mediaPlayer.pause()                        // Ставим на паузу
                isPaused = true                            // Устанавливаем флаг паузы
                stopCassetteAnimation()                    // Останавливаем анимацию
            } else if (isPaused) {
                // Если песня на паузе, сбрасываем mediaPlayer для полной остановки
                mediaPlayer.stop()
                mediaPlayer.prepare()                      // Подготавливаем к следующему воспроизведению
                isPaused = false                           // Сбрасываем флаг паузы
                pausePosition = 0                          // Сбрасываем позицию
            }
        }
    }
}
