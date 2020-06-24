package com.roda.paqueue.ui.queue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roda.paqueue.R
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import com.roda.paqueue.ui.players.setup
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.util.*

fun RecyclerView.setup(fragment: Fragment) {
    this.layoutManager = LinearLayoutManager(fragment.context)
}

class QueueFragment : Fragment(), ListAdapter.OnClickListener {

    private lateinit var queueViewModel: QueueViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ListAdapter
    private val playersPerCourt: Int = 4
    private var TAG = "QueueFragment"

    private val QUEUE_STATUSES = object {
        var IDLE: String = "IDLE"
        var ACTIVE: String = "ACTIVE"
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        queueViewModel =
                ViewModelProviders.of(this).get(QueueViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_queue, container, false)
        adapter = ListAdapter(this.context,this)
        recyclerView = root.findViewById<RecyclerView>(R.id.rvQueues).also { it.setup(this) }
        recyclerView.adapter = adapter
        queueViewModel.getQueues().observe(viewLifecycleOwner, Observer { queues ->
            if(queues.isNotEmpty()) {
                adapter.addQueues(queues)
            }
        })

        val btnGenQueue = root.findViewById<Button>(R.id.btnGenQueue)
        btnGenQueue.setOnClickListener {
            val textNumCourts = root.findViewById<EditText>(R.id.editTextNumCourts).text.toString()
            if(textNumCourts.isEmpty()) {
                Toast.makeText(this.context, "Please provide number of courts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numCourts: Int = Integer.parseInt(textNumCourts)
            val allowedPlayers: Int = numCourts * playersPerCourt
            Realm.getDefaultInstance().use { realm ->
                val checkPlayers = realm.where<Player>().limit(allowedPlayers.toLong()).findAll()
                if(checkPlayers.size < allowedPlayers) {
                    Toast.makeText(this.context, "You do not have enough players for $numCourts courts", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                for(court in 1..numCourts) {
                    realm.executeTransaction {
                        // get all players with less or no queues
                        val playerList = ArrayList<Player>()
                        val players = realm.where<Player>().sort("queue_games")
                            .limit(playersPerCourt.toLong()).findAll()
                        playerList.addAll(players)
                        playerList.shuffle()

                        // check queue for active queue with specific court number
                        val checkQueue = realm.where<Queue>().equalTo("court_number", court)
                            .equalTo("status", QUEUE_STATUSES.ACTIVE).findFirst()

                        // add players to queue
                        val queue = realm.createObject(Queue::class.java, UUID.randomUUID().toString())
                        queue.status = if(checkQueue == null) QUEUE_STATUSES.ACTIVE else QUEUE_STATUSES.IDLE
                        queue.court_number = court
                        queue.players.addAll(playerList)

                        // add queue to players
                        players.forEach { player ->
                            val queueGames = player.queue_games.split('_')
                            val addQueue = Integer.parseInt(queueGames[0]) + 1
                            player.queue_count++
                            player.queue_games = addQueue.toString() + "_" + queueGames[1]
                            player.queue.add(queue)
                        }
                    }
                }
                Toast.makeText(this.context, "Queues generated", Toast.LENGTH_LONG).show()
            }
        }
        return root
    }

    override fun onItemClick(position: Int, itemView: View?) {
        Log.d("test", "onItemClick: clicked")
    }

    override fun onItemLongClick(position: Int, itemView: View?) {
        Log.d("test", "onItemLongClick: clicked")
    }
}
