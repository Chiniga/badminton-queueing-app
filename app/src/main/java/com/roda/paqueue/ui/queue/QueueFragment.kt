package com.roda.paqueue.ui.queue

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roda.paqueue.MainActivity
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
    private var queueMenu: Menu? = null
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
        val imgBtnSubNumCourts = root.findViewById<ImageButton>(R.id.imgBtnSubNumCourts)
        val imgBtnAddNumCourts = root.findViewById<ImageButton>(R.id.imgBtnAddNumCourts)
        imgBtnSubNumCourts.setOnClickListener {
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

        imgBtnAddNumCourts.setOnClickListener {
            Realm.getDefaultInstance().use { realm ->
                var success = false
                realm.executeTransaction {
                    var court = realm.where<Court>().findFirst()
                    if (court == null) {
                        court = realm.createObject()
                    }

                    val addCourt = court.courts + 1
                    val allowedPlayers: Int = addCourt * QueueConstants.PLAYERS_PER_COURT
                    val checkPlayers = realm.where<Player>().equalTo("is_resting", false).limit(allowedPlayers.toLong()).findAll()
                    if(checkPlayers.size < allowedPlayers) {
                        val courtsMsg = if(addCourt == 1) "$addCourt court" else "$addCourt courts"
                        Toast.makeText(this.context, "You do not have enough players for $courtsMsg", Toast.LENGTH_LONG).show()
                    } else {
                        court.courts++
                        textViewTextNumCourts.text = court.courts.toString()
                        success = true
                    }
                }
                val queues = realm.where<Queue>().count().toInt()
                if (queues > 0 && success) {
                    val queueManager = QueueManager(realm, this.context, (activity as MainActivity).shufflePlayers)
                    queueManager.manageCourts()
                }
            }
        }

        Realm.getDefaultInstance().use { realm ->
            val court = realm.where<Court>().findFirst()
            if (court != null) {
                textViewTextNumCourts.text = court.courts.toString()
            }
        }

        val toggleShuffle = root.findViewById<ToggleButton>(R.id.toggleShuffle)
        if((activity as MainActivity).shufflePlayers) {
            toggleShuffle.isChecked = true
        }
        toggleShuffle.setOnCheckedChangeListener { _, isChecked ->
            (activity as MainActivity).shufflePlayers = isChecked
            adapter.setShuffle((activity as MainActivity).shufflePlayers)
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
                val checkPlayers = realm.where<Player>().equalTo("is_resting", false).limit(allowedPlayers.toLong()).findAll()
                if(checkPlayers.size < allowedPlayers) {
                    val courtsMsg = if(numCourts == 1) "$numCourts court" else "$numCourts courts"
                    Toast.makeText(this.context, "You do not have enough players for $courtsMsg", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val queueManager = QueueManager(realm, this.context, (activity as MainActivity).shufflePlayers)
                if (queueManager.generate()) queueMenu?.findItem(R.id.clear_queue)?.isVisible = true
            }
        }

        val layoutAddQueue: ConstraintLayout = root.findViewById(R.id.layoutAddQueue)
        val imgBtnHideInput: ImageButton = root.findViewById(R.id.imgBtnQueueHideInput)
        val imgBtnShowInput: ImageButton = root.findViewById(R.id.imgBtnQueueShowInput)
        imgBtnHideInput.setOnClickListener {
            layoutAddQueue.visibility = View.GONE
            imgBtnHideInput.visibility = View.INVISIBLE
            imgBtnShowInput.visibility = View.VISIBLE

            (activity as MainActivity).queueInputShown = false
        }
        imgBtnShowInput.setOnClickListener {
            layoutAddQueue.visibility = View.VISIBLE
            imgBtnShowInput.visibility = View.INVISIBLE
            imgBtnHideInput.visibility = View.VISIBLE

            (activity as MainActivity).queueInputShown = true
        }

        if(!(activity as MainActivity).queueInputShown) {
            layoutAddQueue.visibility = View.GONE
            imgBtnHideInput.visibility = View.INVISIBLE
            imgBtnShowInput.visibility = View.VISIBLE
        }
        return root
    }

    // enable options menu in this fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.queue_menu, menu)

        Realm.getDefaultInstance().use { realm ->
            val hasQueue = realm.where<Queue>().count()
            if (hasQueue > 0) {
                menu.findItem(R.id.clear_queue)?.isVisible = true
            }
        }

        queueMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // get item id to handle item clicks
        val id = item.itemId
        // handle item clicks
        if (id == R.id.clear_queue) {
            showDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(position: Int, itemView: View?) {
        Log.d("test", "onItemClick: clicked")
    }

    override fun onItemLongClick(position: Int, itemView: View?) {
        Log.d("test", "onItemLongClick: clicked")
    }

    private fun showDialog() {
        lateinit var dialog: AlertDialog
        val builder = AlertDialog.Builder(this.context)

        builder.setTitle("Clear Queue List")
        builder.setMessage("Are you sure you want to clear the queue list?")

        val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    adapter.clearList()
                    queueMenu?.findItem(R.id.clear_queue)?.isVisible = false
                }
            }
        }

        builder.setPositiveButton("YES", dialogClickListener)
        builder.setNeutralButton("CANCEL", dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }
}
