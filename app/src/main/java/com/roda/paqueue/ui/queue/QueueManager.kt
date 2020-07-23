package com.roda.paqueue.ui.queue

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.roda.paqueue.models.Court
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.kotlin.oneOf
import io.realm.kotlin.where
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.log

class QueueManager(private val realm: Realm, private val mContext: Context?) {

    private val LOWER_TIER: IntRange = 2..3
    private val MIDDLE_TIER: Int = 4
    private val UPPER_TIER: IntRange = 5..6

    fun generate(generateOne: Boolean = false) {
        val courts = realm.where<Court>().findFirst()!!
        val allPlayers = realm.where<Player>().findAll()
        val queueCount = if (!generateOne) ceil(allPlayers.size.toDouble() / QueueConstants.PLAYERS_PER_COURT.toDouble()).toInt() else 1
        var success = true
        for (count in 1..queueCount) {
            val activeQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_ACTIVE).count().toInt()
            realm.beginTransaction()
            val nonActiveOnly = activeQueues != 0 && activeQueues < courts.courts
            // get all players with less or no queues
            val playerList = getPlayers(nonActiveOnly)
            if (playerList.size < 4) {
                success = false
                realm.cancelTransaction()
                break
            }
            val queue = realm.createObject(Queue::class.java, UUID.randomUUID().toString())
            val lastQueue = realm.where<Queue>().count().toInt()
            // add players to queue
            queue.court_number = lastQueue + 80
            queue.players.addAll(playerList)

            if (activeQueues == 0 || activeQueues < courts.courts) {
                queue.status = QueueConstants.STATUS_ACTIVE
                queue.court_number = activeQueues + 1
            }

            // add queue to players
            playerList.forEach { player ->
                player.queue_count++
                player.queues_games = player.queues_games + 1
                player.queues.add(queue)
            }
            realm.commitTransaction()
        }
        val msg = if(success) "Queues generated" else "Cannot find appropriate players for queue. Please add more players"
        val msgLength = if(success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        Toast.makeText(mContext, msg, msgLength).show()
        // manageCourts()
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

            if (managedCourts < courts.courts) {
                // generate more games to occupy empty courts
                // courts may be empty if IDLE games have players with ACTIVE games
                generate(true)
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
            generate()
        }
    }

    private fun getPlayers(nonActiveOnly: Boolean): ArrayList<Player> {
        val list = ArrayList<Player>()
        val excludePlayerIds = ArrayList<String>()
        var isSameLevel = false
        // pick 2 players for now
        val twoPlayersQuery = realm.where<Player>()
        if (nonActiveOnly) {
            twoPlayersQuery.not().equalTo("queues.status", QueueConstants.STATUS_ACTIVE)
        }
        val twoPlayers = twoPlayersQuery.sort("queues_games")
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
                val twoMorePlayersQuery = realm.where<Player>()
                if (nonActiveOnly) {
                    twoMorePlayersQuery.not().equalTo("queues.status", QueueConstants.STATUS_ACTIVE)
                }
                val twoMorePlayers = twoMorePlayersQuery.not().oneOf("id", excludePlayerIds.toTypedArray())
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
                val twoMorePlayersQuery = realm.where<Player>()
                if (nonActiveOnly) {
                    twoMorePlayersQuery.not().equalTo("queues.status", QueueConstants.STATUS_ACTIVE)
                }
                val twoMorePlayers = twoMorePlayersQuery.not().oneOf("id", excludePlayerIds.toTypedArray())
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
                val twoMorePlayersQuery = realm.where<Player>()
                if (nonActiveOnly) {
                    twoMorePlayersQuery.not().equalTo("queues.status", QueueConstants.STATUS_ACTIVE)
                }
                val twoMorePlayers = twoMorePlayersQuery.not().oneOf("id", excludePlayerIds.toTypedArray())
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