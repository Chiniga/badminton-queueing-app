package com.roda.paqueue.ui.players

import android.content.Context
import android.media.Rating
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.roda.paqueue.models.Player
import com.roda.paqueue.R
import com.roda.paqueue.SortedListAdapter
import io.realm.Realm

fun RecyclerView.setup(fragment: Fragment) {
    this.layoutManager = LinearLayoutManager(fragment.context)
}

class PlayersFragment : Fragment() {

    private lateinit var playersViewModel: PlayersViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SortedListAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        playersViewModel =
                ViewModelProviders.of(this).get(PlayersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_players, container, false)
        adapter = SortedListAdapter()
        recyclerView = root.findViewById<RecyclerView>(R.id.rvPlayers).also { it.setup(this) }
        recyclerView.adapter = adapter
        playersViewModel.getPlayers().observe(viewLifecycleOwner, Observer {
            // display players
            val players: List<Player> = it
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
                if(player.isValidName(playerName.text.toString()) && playerLevel.rating != 0.0f) {
                    player.name = playerName.text.toString()
                    player.level = playerLevel.rating
                    realm.executeTransaction { r ->
                        r.insert(player)
                    }
                }
            }
        }
        return root
    }
}
