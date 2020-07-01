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
import io.realm.Realm
import io.realm.kotlin.where

class QueueFragment : Fragment(), QueueAdapter.OnClickListener {

    private lateinit var queueViewModel: QueueViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QueueAdapter
    private var TAG = "QueueFragment"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        queueViewModel =
                ViewModelProviders.of(this).get(QueueViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_queue, container, false)
        adapter = QueueAdapter(this.context,this)
        recyclerView = root.findViewById(R.id.rvQueues)
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.adapter = adapter
        queueViewModel.getQueues().observe(viewLifecycleOwner, Observer { queues ->
            if(queues.isNotEmpty()) {
                adapter.submitList(queues)
                adapter.notifyDataSetChanged()
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
            val allowedPlayers: Int = numCourts * QueueConstants.PLAYERS_PER_COURT
            Realm.getDefaultInstance().use { realm ->
                val checkPlayers = realm.where<Player>().limit(allowedPlayers.toLong()).findAll()
                if(checkPlayers.size < allowedPlayers) {
                    Toast.makeText(this.context, "You do not have enough players for $numCourts courts", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val queueManager = QueueManager(realm)
                queueManager.generate(numCourts)
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
