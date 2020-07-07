package com.roda.paqueue.models

import com.roda.paqueue.ui.queue.QueueConstants
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class Queue (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    @Required
    var status: String = QueueConstants.STATUS_IDLE,
    var court_number: Int = 99,
    var created_at: Date = Date(),
    var players: RealmList<Player> = RealmList()
): RealmObject() {
    fun hasSameContents(newItem: Queue?): Boolean {
        if (javaClass != newItem?.javaClass) {
            return false
        }

        if (id != newItem.id) {
            return false
        }

        if (status != newItem.status) {
            return false
        }

        if (court_number != newItem.court_number) {
            return false
        }

        if (created_at != newItem.created_at) {
            return false
        }

        if (players != newItem.players) {
            return false
        }

        return true
    }
}