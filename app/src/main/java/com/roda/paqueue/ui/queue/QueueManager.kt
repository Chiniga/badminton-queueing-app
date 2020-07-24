package com.roda.paqueue.ui.queue

import android.content.Context
import com.roda.paqueue.models.Court
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.kotlin.oneOf
import io.realm.kotlin.where
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

class QueueManager(private val realm: Realm, private val mContext: Context?) {

    private val LOWER_TIER: IntRange = 2..3
    private val MIDDLE_TIER: Int = 4
    private val UPPER_TIER: IntRange = 5..6
    private val COURT_BUFFER: Int = 80

    fun generate() {
        val courts = realm.where<Court>().findFirst()!!
        val allPlayers = realm.where<Player>().findAll()
        val queueCount = ceil(allPlayers.size.toDouble() / QueueConstants.PLAYERS_PER_COURT.toDouble()).toInt()
        for (count in 1..queueCount) {
            val availablePlayers = realm.where<Player>().isEmpty("queues").count().toInt()
            if (availablePlayers < 4) break

            realm.beginTransaction()
            val playerList = getPlayers()
            if (playerList.size < 4) {
                // no more balanced players available to complete 1 game
                realm.cancelTransaction()
                break
            }
            val activeGames = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_ACTIVE).count().toInt()
            val queue = realm.createObject(Queue::class.java, UUID.randomUUID().toString())
            val lastQueue = realm.where<Queue>().count().toInt()
            // add players to queue
            queue.court_number = lastQueue + COURT_BUFFER
            queue.players.addAll(playerList)

            if (activeGames == 0 || activeGames < courts.courts) {
                // set game as ACTIVE
                queue.status = QueueConstants.STATUS_ACTIVE
                queue.court_number = activeGames + 1
            }

            // add queue to players
            playerList.forEach { player ->
                player.queue_count++
                player.queues_games = player.queues_games + 1
                player.queues.add(queue)
            }
            realm.commitTransaction()
        }
    }

    fun manageCourts() {
        var managedCourts = 0
        val courts = realm.where<Court>().findFirst()!!
        val activeQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_ACTIVE).count()
        val idleQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_IDLE)
            .not()
            .equalTo("players.queues.status", QueueConstants.STATUS_ACTIVE)
            .sort("created_at").findAll()

        if (activeQueues < courts.courts) {
            for (court in 1..courts.courts) {
                val findCourt = realm.where<Queue>().equalTo("court_number", court).findFirst()
                if (findCourt == null && idleQueues.isNotEmpty()) {
                    // supply idle queue with missing court number
                    realm.executeTransaction {
                        val idleQueue = idleQueues.first()!!
                        idleQueue.status = QueueConstants.STATUS_ACTIVE
                        idleQueue.court_number = court
                    }
                    managedCourts++
                }
            }

            if (idleQueues.isEmpty()) {
                generate()
            }
        }
    }

    fun clearIdle() {
        val idleQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_IDLE)
            .sort("created_at").findAll()

        if (idleQueues.isNotEmpty()) {
            realm.executeTransaction {
                idleQueues.deleteAllFromRealm()
            }
        }
        generate()
    }

    private fun getPlayers(): ArrayList<Player> {
        val list = ArrayList<Player>()
        val excludePlayerIds = ArrayList<String>()
        var isSameLevel = false
        // pick 2 players for now
        val twoPlayers = realm.where<Player>().isEmpty("queues").sort("queues_games")
            .limit(2).findAll()
        // determine level tier based on their total level
        var levelTotal = 0
        twoPlayers.forEach { player ->
            isSameLevel = levelTotal == player.level.toInt()
            levelTotal += player.level.toInt()
            excludePlayerIds.add(player.id)
        }
        list.addAll(twoPlayers)

        when (levelTotal) {
            in LOWER_TIER -> {
                // 1-1 = opponents with 3 or less total level or opponents have same level
                // 1-2 = opponents with 3 or less total level or 5 total level
                var otherLevelTotal = 0
                val maxTotal = 3
                val twoMorePlayers = realm.where<Player>().isEmpty("queues")
                    .not().oneOf("id", excludePlayerIds.toTypedArray())
                    .sort("queues_games").findAll()
                for(player in twoMorePlayers) {
                    val lvlAdd = otherLevelTotal + player.level.toInt()
                    val cond = lvlAdd <= maxTotal
                    val specialCond = isSameLevel && otherLevelTotal == player.level.toInt() ||
                            !isSameLevel && lvlAdd == 5

                    if(cond || specialCond) {
                        otherLevelTotal += player.level.toInt()
                        list.add(player)
                    }

                    if(list.size == 4) break
                }
            }
            MIDDLE_TIER -> {
                // 1-3 = opponents with 4 total level
                // 2-2 = opponents with 4 total level or opponents have same level
                var otherLevelTotal = 0
                val twoMorePlayers = realm.where<Player>().isEmpty("queues")
                    .not().oneOf("id", excludePlayerIds.toTypedArray())
                    .sort("queues_games").findAll()
                for(player in twoMorePlayers) {
                    val lvlAdd = otherLevelTotal + player.level.toInt()
                    val cond = lvlAdd == MIDDLE_TIER
                    val specialCond = isSameLevel && otherLevelTotal == player.level.toInt()

                    if((list.size == 2) || cond || specialCond) {
                        otherLevelTotal += player.level.toInt()
                        list.add(player)
                    }

                    if(list.size == 4) break
                }
            }
            in UPPER_TIER -> {
                // 2-3 = opponents with 5-6 total level or 3 total level
                // 3-3 = opponents with 5-6 total level or opponents have same level
                var otherLevelTotal = 0
                val totalRange = 5..6
                val twoMorePlayers = realm.where<Player>().isEmpty("queues")
                    .not().oneOf("id", excludePlayerIds.toTypedArray())
                    .sort("queues_games").findAll()
                for (player in twoMorePlayers) {
                    val lvlAdd = otherLevelTotal + player.level.toInt()
                    val cond = lvlAdd in totalRange
                    val specialCond = isSameLevel && otherLevelTotal == player.level.toInt() ||
                            !isSameLevel && lvlAdd == 3

                    if((list.size == 2) || cond || specialCond) {
                        otherLevelTotal += player.level.toInt()
                        list.add(player)
                    }

                    if(list.size == 4) break
                }
            }
        }

        list.shuffle()
        return list
    }
}