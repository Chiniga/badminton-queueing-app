package com.roda.paqueue.ui.queue

import android.content.ContentValues.TAG
import android.util.Log
import android.util.Range
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.oneOf
import io.realm.kotlin.where
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

class QueueManager(private val realm: Realm) {

    private val LOWER_TIER: IntRange = 2..3
    private val MIDDLE_TIER: Int = 4
    private val UPPER_TIER: IntRange = 5..6

    fun generate(courts: Int) {
        val allPlayers = realm.where<Player>().findAll()
        val queueCount = ceil(allPlayers.size.toDouble() / 4.0).toInt()
        for(count in 1..queueCount) {
            realm.executeTransaction {
                // get all players with less or no queues
                val playerList = getPlayers()

                // check queue for active queue with specific court number
                val activeQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_ACTIVE)
                    .findAll()

                val court = if(activeQueues.isEmpty()) 1 else activeQueues.size + 1

                // add players to queue
                val queue = realm.createObject(Queue::class.java, UUID.randomUUID().toString())
                if(court <= courts) {
                    queue.status = QueueConstants.STATUS_ACTIVE
                    queue.court_number = court
                }
                queue.players.addAll(playerList)

                // add queue to players
                playerList.forEach { player ->
                    val queueGames = player.queues_games.split('_')
                    val addQueue = Integer.parseInt(queueGames[0]) + 1
                    player.queue_count++
                    player.queues_games = addQueue.toString() + "_" + queueGames[1]
                    player.queue.add(queue)
                }
            }
        }
    }

    private fun getPlayers(): ArrayList<Player> {
        val list = ArrayList<Player>()
        val excludePlayerIds = ArrayList<String>()
        // pick 2 players for now
        val twoPlayers = realm.where<Player>().sort("queues_games")
            .limit(2).findAll()
        // determine level tier based on their total level
        var levelTotal = 0
        twoPlayers.forEach { player ->
            levelTotal += player.level.toInt()
            excludePlayerIds.add(player.id)
        }
        list.addAll(twoPlayers)
        val twoMorePlayers: RealmResults<Player>?

        when(levelTotal) {
            in LOWER_TIER -> {
                // 1-1 = opponents with 3 or less total level
                // 1-2 = opponents with 3 or less total level
                var otherLevelTotal = 0
                val maxLevel = 2.0f
                val maxTotal = if(levelTotal == 2) 4 else 3
                twoMorePlayers = realm.where<Player>().lessThanOrEqualTo("level", maxLevel)
                    .not().oneOf("id", excludePlayerIds.toTypedArray())
                    .sort("queues_games").findAll()
                for(player in twoMorePlayers) {
                    if((otherLevelTotal + player.level.toInt()) <= maxTotal) {
                        otherLevelTotal += player.level.toInt()
                        list.add(player)
                    }

                    if(list.size == 4) break
                }
            }
            MIDDLE_TIER -> {
                // 1-3 = opponents with 4 total level
                // 2-2 = opponents with 4 total level
                var otherLevelTotal = 0
                twoMorePlayers = realm.where<Player>()
                    .not().oneOf("id", excludePlayerIds.toTypedArray())
                    .sort("queues_games").findAll()
                for(player in twoMorePlayers) {
                    if((list.size == 2) ||
                        (otherLevelTotal + player.level.toInt() == MIDDLE_TIER)) {
                        otherLevelTotal += player.level.toInt()
                        list.add(player)
                    }

                    if(list.size == 4) break
                }
            }
            in UPPER_TIER -> {
                // 2-3 = opponents with 5-6 total level
                // 3-3 = opponents with 5-6 total level
                var otherLevelTotal = 0
                val minLevel = 2.0f
                val minTotal = if(levelTotal == 6) 4 else 5
                twoMorePlayers = realm.where<Player>().greaterThanOrEqualTo("level", minLevel)
                    .not().oneOf("id", excludePlayerIds.toTypedArray())
                    .sort("queues_games").findAll()
                for(player in twoMorePlayers) {
                    if((list.size == 2) ||
                        (((otherLevelTotal + player.level.toInt()) <= 6) &&
                                ((otherLevelTotal + player.level.toInt()) >= minTotal))) {
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