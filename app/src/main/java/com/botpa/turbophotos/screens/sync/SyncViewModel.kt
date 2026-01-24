package com.botpa.turbophotos.screens.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SyncViewModel : ViewModel() {

    //Status
    var connectionStatus by mutableIntStateOf(SyncService.STATUS_OFFLINE)

    //Connect
    var connectName by mutableStateOf("")
    var connectCode by mutableStateOf("")

    //Users
    val users = mutableStateListOf<User>()

    //Logs
    val logs = mutableStateListOf<String>()
    private val _scrollRequest = MutableSharedFlow<Unit>()
    val scrollRequest = _scrollRequest.asSharedFlow()

    fun requestScrollToBottom() {
        viewModelScope.launch {
            _scrollRequest.emit(Unit)
        }
    }

}