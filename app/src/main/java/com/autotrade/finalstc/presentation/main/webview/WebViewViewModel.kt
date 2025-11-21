package com.autotrade.finalstc.presentation.main.webview

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor() : ViewModel() {

    init {
        Log.d("WebViewViewModel", "🔄 WebViewViewModel initialized")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("WebViewViewModel", "🧹 ViewModel cleared")
    }
}
