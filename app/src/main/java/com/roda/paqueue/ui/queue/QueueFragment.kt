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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roda.paqueue.R
import com.roda.paqueue.models.Court
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
            ViewModelProvider(this).get(QueueViewModel::class.java)
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
        val editTextNumCourts = root.findViewById<EditText>(R.id.editTextNumCourts)

        Realm.getDefaultInstance().use { realm ->
            val court = realm.where<Court>().findFirst()
            if(court != null) {
                editTextNumCourts.setText(court.courts.toString())
            }
        }

        val btnGenQueue = root.findViewById<Button>(R.id.btnGenQueue)
        btnGenQueue.setOnClickListener {
            if(editTextNumCourts.text.toString().isEmpty()) {
                Toast.makeText(this.context, "Please provide number of courts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numCourts: Int = Integer.parseInt(editTextNumCourts.text.toString())
            val allowedPlayers: Int = numCourts * QueueConstants.PLAYERS_PER_COURT
            Realm.getDefaultInstance().use { realm ->
                val checkPlayers = realm.where<Player>().limit(allowedPlayers.toLong()).findAll()
                if(checkPlayers.size < allowedPlayers) {
                    Toast.makeText(this.context, "You do not have enough players for $numCourts courts", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val queueManager = QueueManager(realm, this.context)
                queueManager.generate(numCourts)
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
