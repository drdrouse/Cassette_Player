package com.example.cassette_player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SongSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Пример списка песен
        val songList = listOf("Song 1", "Song 2", "Song 3", "Song 4")

        // Настройка ListView
        val listView = findViewById<ListView>(R.id.song_list)
        val adapter = ArrayAdapter(this, R.layout.list_item_song, R.id.song_title, songList)
        listView.adapter = adapter

        // Обработка нажатия на элемент списка
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedSong = songList[position]

            // Возвращаем результат в MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("selected_song", selectedSong)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Закрываем SongSelectionActivity после выбора
        }
    }
}
