package com.example.cassette_player

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SongSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        loadSongs()  // Загружаем и отображаем песни
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }


    private fun loadSongs() {
        val songList = mutableListOf<String>()  // Список для хранения названий песен

        // URI для поиска аудиофайлов
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID
        )

        // Используем ContentResolver для получения данных
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val songTitle = it.getString(titleColumn)
                val songArtist = it.getString(artistColumn)
                val songInfo = "$songTitle - $songArtist"
                songList.add(songInfo)  // Добавляем каждую песню в список
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
            val selectedSong = songList[position]
            val resultIntent = Intent().apply {
                putExtra("selected_song", selectedSong)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()  // Закрываем SongSelectionActivity после выбора
        }
    }
}
