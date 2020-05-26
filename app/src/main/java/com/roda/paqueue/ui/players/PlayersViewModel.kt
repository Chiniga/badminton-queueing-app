package com.roda.paqueue.ui.players

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.roda.paqueue.models.LiveRealmData
import com.roda.paqueue.models.Player
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.*
import java.lang.Exception

class PlayersViewModel : ViewModel() {

    val realm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    fun getPlayers(): LiveData<RealmResults<Player>> {
        return object: LiveRealmData<Player>(RealmConfiguration.Builder().build())  {
            override fun runQuery(realm: Realm): RealmResults<Player> {
                // Called on UI thread
                return realm.where<Player>().findAllAsync()
            }
        }
    }

    override fun onCleared() {
        realm.close()
        super.onCleared()
    }
}