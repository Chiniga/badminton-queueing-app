package com.roda.paqueue.models

import android.content.Context
import android.widget.Toast
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.kotlin.where
import java.util.*

open class Player (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    @Required
    var name: String = "",
    var level: Float = 1.0f,
    var is_resting: Boolean = false,
    var num_games: Int = 0,
    var queue_count: Int = 0,
    var queues_games: Float = 0.0f,
    var balls_used: Int = 0,
    var total_cost: Double = 0.00,
    var is_paid: Boolean = false,
    var created_at: Date = Date(),
    var queues: RealmList<Queue> = RealmList()
): RealmObject() {
    fun isValid(context: Context?, name: String, level: Float, id: String? = null): Boolean {
        // check name syntax
        val regex = "^[A-Za-z ]+\$".toRegex()
        if (!regex.matches(name)) {
            Toast.makeText(context, "Invalid player name $name", Toast.LENGTH_LONG).show()
            return false
        }

        // check if name already exists
        var playerExists: Player?
        Realm.getDefaultInstance().use { realm ->
            playerExists = realm.where<Player>().equalTo("name", name, Case.INSENSITIVE).findFirst()
        }
        if (playerExists != null && playerExists?.id != id) {
            Toast.makeText(context, "$name already exists", Toast.LENGTH_LONG).show()
            return false
        }

        // check level
        if (level == 0.0f) {
            Toast.makeText(context, "Please provide a level", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
}