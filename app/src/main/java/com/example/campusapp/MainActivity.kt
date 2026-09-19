package com.example.campusapp

import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {
    var searchQuery: String = ""
}

class MainActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchEditText = findViewById<EditText>(R.id.searchEditText)

        // 1. Restore state after recreation
        searchEditText.setText(viewModel.searchQuery)

        // 2. Save state as user types
        searchEditText.doAfterTextChanged { text ->
            viewModel.searchQuery = text.toString()
        }
    }
}