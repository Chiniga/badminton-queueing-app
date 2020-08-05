package com.roda.paqueue.ui.queue

import android.content.Context
import android.widget.Toast
import com.roda.paqueue.models.Court
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.kotlin.where
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

class QueueManager(private val realm: Realm, private val mContext: Context?) {

    var stopGenerating = false

    fun generate() {
        val allPlayers = realm.where<Player>().findAll()
        val queueCount = ceil(allPlayers.size.toDouble() / QueueConstants.PLAYERS_PER_COURT.toDouble()).toInt()
        for (count in 1..queueCount) {
            val availablePlayers = realm.where<Player>().isEmpty("queues").count().toInt()
            if (availablePlayers < QueueConstants.PLAYERS_PER_COURT || !create()) break
        }
        manageCourts()
    }

    fun create(players: ArrayList<Player>? = null): Boolean {
        realm.beginTransaction()
        val playerList = players?: getPlayers()
        if (playerList.size < QueueConstants.PLAYERS_PER_COURT) {
            // no more balanced players available to complete 1 game
            Toast.makeText(mContext, "Idle players are incompatible. Unable to generate anymore games.", Toast.LENGTH_LONG).show()
            stopGenerating = true
            realm.cancelTransaction()
            return false
        }
        val queue = realm.createObject(Queue::class.java, UUID.randomUUID().toString())
        val lastQueue = realm.where<Queue>().count().toInt()
        // add players to queue
        queue.court_number = lastQueue + QueueConstants.COURT_BUFFER
        queue.players.addAll(playerList)

        // add queue to players
        playerList.forEach { player ->
            player.queue_count++
            player.queues_games = player.queues_games + 1
            player.queues.add(queue)
        }
        realm.commitTransaction()

        return true
    }

    fun manageCourts() {
        val courts = realm.where<Court>().findFirst()!!
        val activeQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_ACTIVE).count()
        val idleQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_IDLE)
            .sort("created_at").findAll()

        if (activeQueues < courts.courts && idleQueues.isNotEmpty()) {
            for (court in 1..courts.courts) {
                val findCourt = realm.where<Queue>().equalTo("court_number", court).findFirst()
                if (findCourt == null && idleQueues.isNotEmpty()) {
                    // supply idle queue with missing court number
                    realm.executeTransaction {
                        val idleQueue = idleQueues.first()!!
                        idleQueue.status = QueueConstants.STATUS_ACTIVE
                        idleQueue.court_number = court
                    }
                }
            }
        }

        val playersAvailable = realm.where<Player>().isEmpty("queues").count().toInt()
        if (!stopGenerating &&
            activeQueues < courts.courts &&
            idleQueues.isEmpty() &&
            playersAvailable >= QueueConstants.PLAYERS_PER_COURT
        ) {
            generate()
        }
    }

    fun clearIdle() {
        val idleQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_IDLE)
            .sort("created_at").findAll()

        if (idleQueues.isNotEmpty()) {
            realm.executeTransaction {
                idleQueues.deleteAllFromRealm()
            }
            generate()
        }
    }

    private fun getPlayers(): ArrayList<Player> {
        val list = ArrayList<Player>()
        val zeroGames = 0
        val incompatibleTotal = arrayOf(6, 7, 9)
        val addedLatePlayers = realm.where<Player>().equalTo("num_games", zeroGames).count().toInt()
        val players = realm.where<Player>().isEmpty("queues").sort("queues_games").findAll()
        val playerProxyList = ArrayList<Player>()
        var levelTotal = 0
        playerProxyList.addAll(players)

        // only shuffle players if no new players have been added late
        if (addedLatePlayers == 0 || addedLatePlayers == players.size) playerProxyList.shuffle()

        for (player in playerProxyList) {
            if (list.size == QueueConstants.PLAYERS_PER_COURT) break

            val levelAdd = levelTotal + player.level.toInt()
            if (list.size < 3 || !incompatibleTotal.contains(levelAdd)) {
                levelTotal += player.level.toInt()
                list.add(player)
            }
        }
        list.shuffle()
        return list
    }
}