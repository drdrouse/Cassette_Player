package com.example.cassette_player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SongSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Загрузка песен и настройка интерфейса
        loadSongs()

        // Установка полноэкранного режима
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    /**
     * Метод для загрузки списка песен и их отображения в ListView
     */
    private fun loadSongs() {
        val songList = mutableListOf<String>()  // Список названий песен для отображения
        val songs = mutableListOf<Song>()  // Список объектов Song для хранения данных

        // Заранее определённый список песен (с URI ресурсов)
        val predefinedSongs = listOf(
            Pair("Pink Floyd - Another Brick In The Wall (Original)", "android.resource://${packageName}/${R.raw.wall}")
        )

        // Формируем списки для отображения
        for ((title, resourceUri) in predefinedSongs) {
            songList.add(title)
            songs.add(Song(title, "Unknown Artist", resourceUri))
        }

        val listView = findViewById<ListView>(R.id.song_recycler_view)
        val adapter = ArrayAdapter(this, R.layout.list_item_song, R.id.song_title, songList)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedSong = songs[position]

            val resultIntent = Intent()
            resultIntent.putExtra("song_path", selectedSong.path)
            resultIntent.putExtra("song_title", selectedSong.title)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    /**
     * Класс для представления данных о песне
     */
    data class Song(
        val title: String,
        val artist: String,
        val path: String
    )
}
