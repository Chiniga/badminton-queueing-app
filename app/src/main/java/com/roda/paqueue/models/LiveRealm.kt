package com.roda.paqueue.models

import androidx.lifecycle.LiveData
import io.realm.*

/**
 * Class connecting the Realm lifecycle to that of LiveData objects.
 * Realm will remain open for as long as any LiveData objects are being observed.
 */
abstract class LiveRealmData<T: RealmModel>(val config: RealmConfiguration) : LiveData<RealmResults<T>>() {

    private val listener = RealmChangeListener<RealmResults<T>> { results -> value = results }
    private lateinit var realm: Realm
    private var results: RealmResults<T>? = null

    final override fun onActive() {
        realm = Realm.getInstance(config)
        results = runQuery(realm)
        results!!.addChangeListener(listener)
        value = results;
    }
    final override fun onInactive() {
        results!!.removeAllChangeListeners()
        results = null
        realm.close()
    }

    abstract fun runQuery(realm: Realm): RealmResults<T>
}