package com.example.cassette_player

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


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
    private val rewindInterval = 2000 // Интервал перемотки назад (2 секунды)
    private val rewindHandler = Handler(Looper.getMainLooper())
    private val CHANNEL_ID = "music_notification_channel"
    // Настройки скоростей анимации
    private val normalFrameDelay = 30L   // Ускоренная обычная скорость
    private val fastFrameDelay = 15L     // Более быстрая скорость для перемотки

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            } else {
                // Разрешение уже предоставлено
                createNotificationChannel()
            }
        } else {
            // Для старых версий Android
            createNotificationChannel()
        }

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

    // Задача для перемотки назад
    private val rewindRunnable = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized) {
                val rewindInterval = 300 // Интервал перемотки в миллисекундах (например, 2 секунды)
                val currentPosition = mediaPlayer.currentPosition
                mediaPlayer.seekTo((currentPosition - rewindInterval).coerceAtLeast(0)) // Перематываем назад, не меньше 0
                rewindHandler.postDelayed(this, 50) // Повторяем задачу каждые 500 мс
            }
        }
    }

    private fun handleRewindBackTouchEvent(event: MotionEvent, sound: MediaPlayer, button: ImageButton): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                playSoundOnce(playSound)
                showedToastForNoSong = false
                button.alpha = 0.75f // Затемняем кнопку при нажатии

                if (currentSongPath == null) {
                    Toast.makeText(this, "Песня не выбрана", Toast.LENGTH_SHORT).show()
                    showedToastForNoSong = true
                    return true
                }

                if (isPaused) {
                    Toast.makeText(this, "Песня на паузе. Нажмите воспроизведение для начала перемотки.", Toast.LENGTH_SHORT).show()
                    return true
                }

                if (!sound.isPlaying) {
                    sound.start()
                    sound.isLooping = true
                }

                // Начинаем перемотку назад при удерживании кнопки
                if (!isRewinding && ::mediaPlayer.isInitialized) {
                    isRewinding = true

                    // Ставим песню на паузу перед началом перемотки
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                    animateFramesReversed()
                    rewindHandler.post(rewindRunnable) // Запускаем задачу перемотки
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.alpha = 1.0f // Возвращаем кнопку к нормальному состоянию

                if (sound.isPlaying) {
                    sound.pause()
                    sound.seekTo(0)
                    sound.isLooping = false
                }

                // Останавливаем перемотку назад
                if (isRewinding) {
                    isRewinding = false
                    rewindHandler.removeCallbacks(rewindRunnable) // Останавливаем задачу перемотки

                    // Возобновляем воспроизведение, если песня была выбрана
                    if (::mediaPlayer.isInitialized && !isPaused) {
                        mediaPlayer.start() // Продолжаем воспроизведение с текущей позиции
                    }
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
                playSoundOnce(playSound)
                // Сбрасываем флаг при новом нажатии
                showedToastForNoSong = false
                button.alpha = 0.75f // Затемняем кнопку при нажатии

                // Проверка, выбрана ли песня, если нет - не запускаем перемотку
                if (currentSongPath == null) {
                    Toast.makeText(this, "Песня не выбрана", Toast.LENGTH_SHORT).show()
                    showedToastForNoSong = true
                    return true // Не выполняем действий, если песня не выбрана
                }

                // Если песня на паузе, показываем уведомление и выходим из метода
                if (isPaused) {
                    Toast.makeText(this, "Песня на паузе. Нажмите воспроизведение для начала перемотки.", Toast.LENGTH_SHORT).show()
                    return true
                }

                // Запускаем звук перемотки и включаем его зацикливание
                if (!sound.isPlaying) {
                    sound.start()
                    sound.isLooping = true // Зацикливаем звук перемотки
                }

                // Запускаем анимацию перемотки вперед, если она еще не запущена
                if (!isFastForwarding && ::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    isFastForwarding = true
                    isAnimationRunning = true
                    animateFramesForward() // Запускаем анимацию перемотки вперед
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.alpha = 1.0f // Возвращаем кнопку к нормальному состоянию

                // Останавливаем звук перемотки и выключаем зацикливание
                if (sound.isPlaying) {
                    sound.pause()
                    sound.seekTo(0)
                    sound.isLooping = false
                }

                // Если кнопка отпущена и песня выбрана, останавливаем перемотку и продолжаем проигрывание
                if (isFastForwarding && currentSongPath != null) {
                    isFastForwarding = false

                    // Возобновляем воспроизведение, если оно было остановлено
                    if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                        mediaPlayer.start() // Продолжаем воспроизведение с новой позиции
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
                try {
                    stopSong() // Останавливаем текущую песню, если она играет
                    if (!::mediaPlayer.isInitialized) {
                        mediaPlayer = MediaPlayer() // Создаём новый экземпляр MediaPlayer, если он не существует
                    }

                    mediaPlayer.reset() // Сбрасываем MediaPlayer перед загрузкой новой песни

                    // Устанавливаем данные для новой песни
                    if (songPath.startsWith("android.resource://")) {
                        val uri = Uri.parse(songPath)
                        val afd = contentResolver.openAssetFileDescriptor(uri, "r")!!
                        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } else {
                        mediaPlayer.setDataSource(songPath)
                    }

                    mediaPlayer.prepare() // Предварительно подготавливаем песню, но не запускаем
                    currentSongPath = songPath
                    currentSongTitle = songTitle
                    updateCassetteLabel(songTitle) // Обновляем отображение кассеты

                    // Вызов метода для обновления уведомления
                    updateNotification(songTitle)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Ошибка при выборе песни", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Ошибка: Песня не выбрана", Toast.LENGTH_SHORT).show()
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
            if (!::mediaPlayer.isInitialized || currentSongPath.isNullOrEmpty()) {
                Toast.makeText(this, "Сначала выберите песню", Toast.LENGTH_SHORT).show()
                return
            }

            if (!mediaPlayer.isPlaying) {
                // Всегда запускаем воспроизведение с начала, если новая песня
                mediaPlayer.seekTo(0)
                mediaPlayer.start() // Запускаем воспроизведение
                startCassetteAnimation() // Запуск анимации
                isPaused = false

                updateNotification(currentSongTitle)
            } else {
                Toast.makeText(this, "Песня уже воспроизводится", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Не удалось воспроизвести песню", Toast.LENGTH_SHORT).show()
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

                clearNotification()
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Player Notifications"
            val descriptionText = "Displays the current song playing"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(songTitle: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // Замените на ваш значок
            .setContentTitle("Playing Now")
            .setContentText(songTitle ?: "No song playing")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Уведомление не исчезает свайпом

        notificationManager.notify(1, builder.build())
    }

    private fun clearNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
                createNotificationChannel()
            } else {
                Toast.makeText(this, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
