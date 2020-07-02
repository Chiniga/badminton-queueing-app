package com.roda.paqueue.ui.players

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.roda.paqueue.models.LiveRealmData
import com.roda.paqueue.models.Player
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.*

class PlayersViewModel : ViewModel() {

    val realm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    fun getPlayers(): LiveData<RealmResults<Player>> {
        return object: LiveRealmData<Player>()  {
            override fun runQuery(realm: Realm): RealmResults<Player> {
                // Called on UI thread
                return realm.where<Player>().findAllAsync()
            }
        }
    }
}