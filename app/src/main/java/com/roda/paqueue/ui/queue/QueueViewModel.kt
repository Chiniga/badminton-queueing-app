package com.roda.paqueue.ui.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.roda.paqueue.models.LiveRealmData
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.where

class QueueViewModel : ViewModel() {

    val realm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    fun getQueues(): LiveData<RealmResults<Player>> {
        return object: LiveRealmData<Player>(RealmConfiguration.Builder().build())  {
            override fun runQuery(realm: Realm): RealmResults<Player> {
                // Called on UI thread
                return realm.where<Player>().findAllAsync()
            }
        }
    }
}