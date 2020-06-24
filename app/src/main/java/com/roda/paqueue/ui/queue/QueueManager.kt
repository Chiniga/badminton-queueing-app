package com.roda.paqueue.ui.queue

import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.kotlin.where
import java.util.*
import kotlin.collections.ArrayList

class QueueManager(private val realm: Realm) {

    fun generate(courts: Int) {
        for(court in 1..courts) {
            realm.executeTransaction {
                // get all players with less or no queues
                val playerList = getPlayers()

                // check queue for active queue with specific court number
                val checkQueue = realm.where<Queue>().equalTo("court_number", court)
                    .equalTo("status", QueueConstants.STATUS_ACTIVE).findFirst()

                // add players to queue
                val queue = realm.createObject(Queue::class.java, UUID.randomUUID().toString())
                if(checkQueue == null) {
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
        val players = realm.where<Player>().sort("queue_games")
            .limit(QueueConstants.PLAYERS_PER_COURT.toLong()).findAll()
        // add level logic here
        list.addAll(players)
        list.shuffle()
        return list
    }
}