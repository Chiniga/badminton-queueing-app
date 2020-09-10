package com.roda.paqueue.ui.players

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roda.paqueue.models.Player
import com.roda.paqueue.R
import com.roda.paqueue.models.Court
import com.roda.paqueue.ui.queue.QueueConstants
import com.roda.paqueue.ui.queue.QueueManager
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlin.collections.ArrayList

class PlayersFragment : Fragment(), PlayerListAdapter.OnClickListener {

    private lateinit var playersViewModel: PlayersViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlayerListAdapter
    private var actionMode: ActionMode? = null
    private var playerMenu: Menu? = null
    private var createQueueMenu: Menu? = null
    private var playerList: ArrayList<Player> = ArrayList()
    private var itemViewArrayList: ArrayList<View?> = ArrayList()
    private var isDeleteModeActive: Boolean = false
    private var isQueueModeActive: Boolean = false
    private val TAG = "PlayersFragment"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        playersViewModel =
                ViewModelProvider(this).get(PlayersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_players, container, false)
        registerForContextMenu(root)
        adapter = PlayerListAdapter(this.context,this)
        recyclerView = root.findViewById(R.id.rvPlayers)
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.adapter = adapter
        playersViewModel.getPlayers().observe(viewLifecycleOwner, Observer { players ->
            // display players
            if(players.isNotEmpty()) {
                adapter.addPlayers(players)
            }
        })

        val btnAddPlayer: ImageButton = root.findViewById(R.id.imgBtnAddPlayer)
        btnAddPlayer.setOnClickListener {
            val playerName = root.findViewById<EditText>(R.id.editTextPlayerName)
            val playerLevel = root.findViewById<RatingBar>(R.id.ratingBar)
            Realm.getDefaultInstance().use { realm ->
                val player = Player()
                if(player.isValid(this.context, playerName.text.toString(), playerLevel.rating)) {
                    // get lowest queue count and add to newly added player to balance things out
                    val lowestQueueCount = realm.where<Player>().sort("queue_count").findFirst()
                    lowestQueueCount?.let {
                        player.queue_count = it.queue_count
                        player.queues_games = it.queue_count.toFloat()
                    }
                    player.name = playerName.text.toString()
                    player.level = playerLevel.rating
                    realm.executeTransaction { r ->
                        r.insert(player)
                    }
                    Toast.makeText(this.context, playerName.text.toString() + " has been added", Toast.LENGTH_SHORT).show()
                    playerName.setText("")
                    playerLevel.rating = 0.0f
                }
            }
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
        inflater.inflate(R.menu.player_menu, menu)

        playerMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle menu item clicks
        when (item.itemId) {
            R.id.create_queue -> {
                if(actionMode == null) {
                    isQueueModeActive = true
                    requireActivity().startActionMode(CreateQueueActionModeCallback())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(position: Int, itemView: View?) {
        if (isDeleteModeActive || isQueueModeActive) {
            val color: Int? = (itemView?.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
            if (color == 0) {
                val newColor = if(isDeleteModeActive) R.color.redBg else R.color.greenBg
                itemView?.setBackgroundColor(requireActivity().getColor(newColor))
                itemViewArrayList.add(itemView)
                playerList.add(adapter.getPlayer(position))
            } else {
                itemView?.setBackgroundColor(Color.TRANSPARENT)
                itemViewArrayList.remove(itemView)
                playerList.remove(adapter.getPlayer(position))
            }

            createQueueMenu?.findItem(R.id.done_create_queue)?.isVisible = false
            if(isQueueModeActive && (playerList.size == QueueConstants.PLAYERS_PER_COURT)) {
                createQueueMenu?.findItem(R.id.done_create_queue)?.isVisible = true
            }
            if (itemViewArrayList.isEmpty()) {
                actionMode?.finish()
            }
        }
    }

    override fun onItemLongClick(position: Int, itemView: View?) {
        if (isQueueModeActive) return

        if (!isDeleteModeActive) {
            // activate delete mode
            itemView?.setBackgroundColor(requireActivity().getColor(R.color.redBg))
            itemViewArrayList.add(itemView)
            playerList.add(adapter.getPlayer(position))
            isDeleteModeActive = true

            // activate ActionMode
            if(actionMode == null) {
                requireActivity().startActionMode(DeleteActionModeCallback())
            }
        } else {
            // deactivate delete mode
            actionMode?.finish()
        }
    }

    private fun deactivateActionMode() {
        isDeleteModeActive = false
        isQueueModeActive = false
        playerList = ArrayList()
        itemViewArrayList.forEach { item ->
            item?.setBackgroundColor(Color.TRANSPARENT)
        }
        itemViewArrayList = ArrayList()
    }

    inner class DeleteActionModeCallback: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.player_delete_menu, menu)

            mode?.title = "Delete Mode"
            mode?.subtitle = ""
            actionMode = mode

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when(item?.itemId) {
                R.id.delete_player -> {
                    playerList.forEach { player ->
                        adapter.removePlayer(player)
                    }
                    actionMode?.finish()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            deactivateActionMode()
            actionMode = null
        }
    }

    inner class CreateQueueActionModeCallback: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.player_create_queue_menu, menu)
            createQueueMenu = menu

            mode?.title = "Set Mode"
            mode?.subtitle = "Create custom game"
            actionMode = mode

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when(item?.itemId) {
                R.id.done_create_queue -> {
                    Realm.getDefaultInstance().use { realm ->
                        val court = realm.where<Court>().findFirst()
                        if (court == null) {
                            realm.executeTransaction {
                                // add court if no available courts yet
                                val newCourt: Court = realm.createObject()
                                newCourt.courts++
                            }
                        }
                        val queueManager = QueueManager(realm, parentFragment?.context)
                        queueManager.create(playerList)
                        queueManager.manageCourts()
                    }
                    Toast.makeText(parentFragment?.context, "Queue created", Toast.LENGTH_SHORT).show()
                    actionMode?.finish()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            deactivateActionMode()
            actionMode = null
        }
    }
}