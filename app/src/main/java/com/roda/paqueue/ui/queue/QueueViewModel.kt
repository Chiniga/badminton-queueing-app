package com.roda.paqueue.ui.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.roda.paqueue.models.LiveRealmData
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where

class QueueViewModel : ViewModel() {

    val realm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    fun getQueues(): LiveData<RealmResults<Queue>> {
        return object: LiveRealmData<Queue>()  {
            override fun runQuery(realm: Realm): RealmResults<Queue> {
                // Called on UI thread
                return realm.where<Queue>().sort("court_number").findAllAsync()
            }
        }
    }
}