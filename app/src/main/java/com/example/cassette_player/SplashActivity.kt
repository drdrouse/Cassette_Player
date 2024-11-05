package com.example.cassette_player

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Установка полноэкранного режима
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_splash)

        // Запуск фоновой загрузки данных
        loadDataAndStartMainActivity()
    }

    // Метод для загрузки данных и перехода на главный экран
    private fun loadDataAndStartMainActivity() {
        // Асинхронная загрузка данных с использованием Coroutine
        GlobalScope.launch(Dispatchers.IO) {
            // Здесь может быть вызов функции, которая загружает данные
            delay(3000) // Симуляция задержки для загрузки данных

            // Переход на основной экран
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish() // Закрываем SplashActivity
            }
        }
    }
}
