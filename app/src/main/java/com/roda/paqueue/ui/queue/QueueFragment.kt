package com.roda.paqueue.ui.queue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roda.paqueue.R
import com.roda.paqueue.models.Court
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class QueueFragment : Fragment(), QueueListAdapter.OnClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QueueListAdapter
    private var TAG = "QueueFragment"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_queue, container, false)
        adapter = QueueListAdapter(this.context,this, Realm.getDefaultInstance().where<Queue>().sort("court_number").findAllAsync())
        recyclerView = root.findViewById(R.id.rvQueues)
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.adapter = adapter
        val textViewTextNumCourts = root.findViewById<TextView>(R.id.textViewNumCourts)
        val btnSubNumCourts = root.findViewById<Button>(R.id.btnSubNumCourts)
        val btnAddNumCourts = root.findViewById<Button>(R.id.btnAddNumCourts)

        btnSubNumCourts.setOnClickListener {
            Realm.getDefaultInstance().use { realm ->
                realm.executeTransaction {
                    val court = realm.where<Court>().findFirst()

                    if (court != null && court.courts > 0) {
                        court.courts--
                        textViewTextNumCourts.text = court.courts.toString()
                    }
                }
            }
        }

        btnAddNumCourts.setOnClickListener {
            Realm.getDefaultInstance().use { realm ->
                realm.executeTransaction {
                    var court = realm.where<Court>().findFirst()
                    if (court == null) {
                        court = realm.createObject()
                    }

                    if (court.courts < 20) {
                        court.courts++
                        textViewTextNumCourts.text = court.courts.toString()
                    }
                }
            }
        }

        Realm.getDefaultInstance().use { realm ->
            val court = realm.where<Court>().findFirst()
            if(court != null) {
                textViewTextNumCourts.text = court.courts.toString()
            }
        }

        val btnGenQueue = root.findViewById<Button>(R.id.btnGenQueue)
        btnGenQueue.setOnClickListener {
            if(textViewTextNumCourts.text.toString() == "0") {
                Toast.makeText(this.context, "No courts available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numCourts: Int = Integer.parseInt(textViewTextNumCourts.text.toString())
            val allowedPlayers: Int = numCourts * QueueConstants.PLAYERS_PER_COURT
            Realm.getDefaultInstance().use { realm ->
                val checkPlayers = realm.where<Player>().limit(allowedPlayers.toLong()).findAll()
                if(checkPlayers.size < allowedPlayers) {
                    val courtsMsg = if(numCourts == 1) "$numCourts court" else "$numCourts courts"
                    Toast.makeText(this.context, "You do not have enough players for $courtsMsg", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val queueManager = QueueManager(realm, this.context)
                queueManager.generate()
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
