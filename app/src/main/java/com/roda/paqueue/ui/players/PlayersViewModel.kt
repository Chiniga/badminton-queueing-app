package com.roda.paqueue.ui.players

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayersViewModel : ViewModel() {

    private val _players = MutableLiveData<List<Player>>().apply {
        loadPlayers()
    }

    fun getPlayers(): LiveData<List<Player>> {
        return _players
    }

    private fun loadPlayers() {

    }
}