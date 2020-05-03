package com.roda.paqueue.ui.players

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
            Log.d("Player log david", players.toString())
            if(players.isNotEmpty()) {
                adapter.addPlayers(players)
            }
        })

        val btnAddPlayer: Button = root.findViewById(R.id.button)
        btnAddPlayer.setOnClickListener {
            Realm.getDefaultInstance().use { realm ->
                val player = Player()
                player.name = "Roma hernandez"
                player.level = "advanced"
                // supply player name and level
                realm.executeTransaction { realm ->
                    realm.insert(player)
                }
            }
        }
        return root
    }

    fun addPlayer() {
        Realm.getDefaultInstance().use { realm ->
            val player = Player()
            player.name = "Roma hernandez"
            player.level = "advanced"
            // supply player name and level
            realm.executeTransaction { realm ->
                realm.insert(player)
            }
        }
    }
}
