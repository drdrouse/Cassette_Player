package com.example.cassette_player

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SongSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        loadSongs()  // Загружаем и отображаем песни
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun loadSongs() {
        val songList = mutableListOf<String>()  // Список для отображения названий песен
        val songs = mutableListOf<Song>()  // Список объектов Song для хранения пути и названия

        // URI для поиска аудиофайлов
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA // Полный путь к файлу
        )

        // Используем ContentResolver для получения данных
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val songTitle = it.getString(titleColumn)
                val songArtist = it.getString(artistColumn)
                val songPath = it.getString(dataColumn)

                val songInfo = "$songTitle - $songArtist"
                songList.add(songInfo)  // Добавляем информацию о песне для отображения
                songs.add(Song(songTitle, songArtist, songPath))  // Добавляем песню в список с полным путем
            }
        }

        if (songList.isEmpty()) {
            Toast.makeText(this, "Не удалось найти музыкальные файлы", Toast.LENGTH_SHORT).show()
        }

        // Настройка ListView с адаптером
        val listView = findViewById<ListView>(R.id.song_recycler_view)
        val adapter = ArrayAdapter(this, R.layout.list_item_song, R.id.song_title, songList)
        listView.adapter = adapter

        // Обработка нажатия на элемент списка
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedSong = songs[position]
            val songTitle = "${selectedSong.title} - ${selectedSong.artist}"  // Название, отображаемое в списке

            val resultIntent = Intent().apply {
                putExtra("song_path", selectedSong.path)      // Полный путь к файлу
                putExtra("song_title", songTitle)    // Название, которое отображается в списке
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()  // Закрываем SongSelectionActivity после выбора
        }
    }

    // Класс для хранения информации о песне
    data class Song(val title: String, val artist: String, val path: String)
}
