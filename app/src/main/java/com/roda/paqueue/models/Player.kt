package com.roda.paqueue.models

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class Player (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    @Required
    var name: String = "",
    var level: Float = 1.0f,
    var num_games: Int = 0,
    var created_at: Date = Date(),
    var queues: RealmList<Queue>? = RealmList()
): RealmObject() {
    fun isValidName(name: String): Boolean {
        val regex = "^[A-Za-z ]+\$".toRegex()
        if(!regex.matches(name)) {
            return false;
        }
        return true;
    }
}