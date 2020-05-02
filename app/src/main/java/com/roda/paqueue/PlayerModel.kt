package com.roda.paqueue

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.kotlin.*

open class PlayerModel (
    @PrimaryKey var id: Long = 0,
    @Required var name: String = "",
    @Required var level: String = "",
    var num_games: Int = 0
): RealmObject() {
    fun isValidName(name: String): Boolean {
        val regex = "/^[A-Za-z]+\$/".toRegex()
        if(!regex.matches(name)) {
            return false;
        }
        return true;
    }
}