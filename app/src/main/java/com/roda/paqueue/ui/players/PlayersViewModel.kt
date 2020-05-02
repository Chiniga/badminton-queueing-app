package com.roda.paqueue.ui.players

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import PlayerModel
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.*
import java.lang.Exception

class PlayersViewModel : ViewModel() {

    private val _players = MutableLiveData<List<PlayerModel>>().apply {
        loadPlayers()
    }

    fun getPlayers(): LiveData<List<PlayerModel>> {
        return _players
    }

    private fun loadPlayers() {
        try {
            val realm = Realm.getDefaultInstance()
            realm.where<PlayerModel>().findAll().sort("created_at", Sort.ASCENDING)
        } catch (error: Exception) {
            Log.e("Load Player Error", error.message)
        }
    }
}