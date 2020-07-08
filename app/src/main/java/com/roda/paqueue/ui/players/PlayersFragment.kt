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
import io.realm.Realm
import io.realm.kotlin.where

class PlayersFragment : Fragment(), PlayerListAdapter.OnClickListener {

    private lateinit var playersViewModel: PlayersViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlayerListAdapter
    private var playerMenu: Menu? = null
    private var playerDeleteList: ArrayList<Player> = ArrayList()
    private var itemViewArrayList: ArrayList<View?> = ArrayList()
    private var isDeleteActive: Boolean = false
    private val TAG = "PlayersFragment"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        playersViewModel =
                ViewModelProvider(this).get(PlayersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_players, container, false)
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
                        player.queues_games = player.queues_games + it.queue_count
                    }
                    player.name = playerName.text.toString()
                    player.level = playerLevel.rating
                    realm.executeTransaction { r ->
                        r.insert(player)
                    }
                    Toast.makeText(this.context, playerName.text.toString() + " has been added", Toast.LENGTH_LONG).show()
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
        inflater.inflate(R.menu.main_menu, menu)

        menu.findItem(R.id.clear_queue).isVisible = false
        menu.findItem(R.id.delete_player).isVisible = false

        playerMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // get item id to handle item clicks
        val id = item.itemId
        // handle item clicks
        if (id == R.id.delete_player) {
            playerDeleteList.forEach { player ->
                adapter.removePlayer(player)
            }
            deactivateDeleteMode()
            Toast.makeText(this.context, "Delete successful", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(position: Int, itemView: View?) {
        if (isDeleteActive) {
            val color: Int? = (itemView?.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
            if (color == 0) {
                itemView?.setBackgroundColor(requireActivity().getColor(R.color.redBg))
                itemViewArrayList.add(itemView)
                playerDeleteList.add(adapter.getPlayer(position))
            } else {
                itemView?.setBackgroundColor(Color.TRANSPARENT)
                itemViewArrayList.remove(itemView)
                playerDeleteList.remove(adapter.getPlayer(position))
            }
            if (itemViewArrayList.isEmpty()) {
                deactivateDeleteMode()
            }
        }
    }

    override fun onItemLongClick(position: Int, itemView: View?) {
        if (!isDeleteActive) {
            // activate delete mode
            itemView?.setBackgroundColor(requireActivity().getColor(R.color.redBg))
            itemViewArrayList.add(itemView)
            playerDeleteList.add(adapter.getPlayer(position))
            activateDeleteMode()
        } else {
            // deactivate delete mode
            itemViewArrayList.forEach { item ->
                item?.setBackgroundColor(Color.TRANSPARENT)
            }
            deactivateDeleteMode()
        }
    }

    private fun deactivateDeleteMode() {
        isDeleteActive = false
        playerMenu?.findItem(R.id.delete_player)?.isVisible = false
        playerMenu?.findItem(R.id.search_player)?.isVisible = true
        playerDeleteList = ArrayList()
        itemViewArrayList = ArrayList()
    }

    private fun activateDeleteMode() {
        isDeleteActive = true
        playerMenu?.findItem(R.id.delete_player)?.isVisible = true
        playerMenu?.findItem(R.id.search_player)?.isVisible = false
    }
}