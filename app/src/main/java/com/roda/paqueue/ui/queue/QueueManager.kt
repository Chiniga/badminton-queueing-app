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

class QueueManager(private val realm: Realm, private val mContext: Context?, private val shufflePlayers: Boolean?) {

    var stopGenerating = false

    fun generate(): Boolean {
        var success = false
        while (true) {
            val availablePlayers = realm.where<Player>().equalTo("is_resting", false).isEmpty("queues").count().toInt()
            if (availablePlayers < QueueConstants.PLAYERS_PER_COURT || !create()) break
            success = true
        }
        manageCourts()
        return success
    }

    fun create(players: ArrayList<Player>? = null): Boolean {
        realm.beginTransaction()
        var playerList = players?: getPlayers()
        if (playerList.size < QueueConstants.PLAYERS_PER_COURT) {
            // no more balanced players available to complete 1 game
            Toast.makeText(mContext, "Idle players are incompatible. Add more players or clear more games.", Toast.LENGTH_LONG).show()
            stopGenerating = true
            realm.cancelTransaction()
            return false
        }
        playerList = arrangePlayers(playerList)
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

    private fun getPlayers(): ArrayList<Player> {
        val list = ArrayList<Player>()
        // based on calculations when summing up player levels,
        // the numbers 7 and 9 are the most incompatible or unideal player level combinations
        // ex. 2-3-1-1 | 2-3-2-2 | 3-3-2-1
        val incompatibleTotal = arrayOf(7, 9)
        // numbers 6 and 10 can be accepted with additional conditions
        // RULES: if total == 6, then game should not have level 3 player
        // ex. (acceptable) 2-2-1-1 | (unacceptable) 1-1-3-1
        //        if total == 10, then game should not have level 1 player
        // ex. (acceptable) 2-3-2-3 | (unacceptable) 3-3-3-1
        val specialIncompatibleTotal = arrayOf(6, 10)
        val players = realm.where<Player>().equalTo("is_resting", false).isEmpty("queues").sort("queues_games").findAll()
        val playerProxyList = ArrayList<Player>()
        var levelTotal = 0
        var hasLevelOne = false
        var hasLevelThree = false
        playerProxyList.addAll(players)

        // shuffle players if shuffling is enabled
        if (shufflePlayers != null && shufflePlayers) playerProxyList.shuffle()

        for (player in playerProxyList) {
            if (list.size == QueueConstants.PLAYERS_PER_COURT) break

            val levelAdd = levelTotal + player.level.toInt()
            val specialCondOne = levelAdd == 6 && !hasLevelThree && player.level.toInt() != 3
            val specialCondTwo = levelAdd == 10 && !hasLevelOne && player.level.toInt() != 1
            if (list.size < 3 ||
                (!incompatibleTotal.contains(levelAdd) && !specialIncompatibleTotal.contains(levelAdd)) ||
                specialCondOne || specialCondTwo
                ) {
                levelTotal += player.level.toInt()
                list.add(player)

                if (player.level.toInt() == 3) hasLevelThree = true
                if (player.level.toInt() == 1) hasLevelOne = true
            }
        }

        return list
    }

    private fun arrangePlayers(list: ArrayList<Player>): ArrayList<Player> {
        val foundLevelThree = list.find { it.level == 3.0f } != null
        val foundLevelOne = list.find { it.level == 1.0f } != null
        var isSorted = false
        // check level 1 and 3 players first since they MUST be partners
        for (player in list) {
            if ((player.level == 1.0f && foundLevelThree) ||
                (player.level == 3.0f && foundLevelOne)) {
                val findLevel = if(player.level == 3.0f && foundLevelOne) 1.0f else 3.0f
                val movePlayer = list.find { it.level == findLevel }
                if (movePlayer != null) {
                    list.remove(movePlayer)
                    list.remove(player)
                    list.add(movePlayer)
                    list.add(player)
                    isSorted = true
                }
                break
            }
        }

        if (!isSorted) {
            // check for level 2 player with possible level 1 or level 3 partner
            for (player in list) {
                if ((player.level == 2.0f && foundLevelThree) ||
                    (player.level == 2.0f && foundLevelOne)) {
                    val findLevel = if(foundLevelOne) 1.0f else 3.0f
                    val movePlayer = list.find { it.level == findLevel }
                    if (movePlayer != null) {
                        list.remove(movePlayer)
                        list.remove(player)
                        list.add(movePlayer)
                        list.add(player)
                    }
                    break
                }
            }
        }

        return list
    }
}