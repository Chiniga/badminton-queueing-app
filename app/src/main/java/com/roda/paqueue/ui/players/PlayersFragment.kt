package com.roda.paqueue.ui.players

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.roda.paqueue.models.Player
import com.roda.paqueue.R
import io.realm.Realm

class PlayersFragment : Fragment() {

    private lateinit var playersViewModel: PlayersViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        playersViewModel =
                ViewModelProviders.of(this).get(PlayersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_players, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        playersViewModel.getPlayers().observe(viewLifecycleOwner, Observer {
            // display players
            val players: List<Player> = it
            Log.d("Player log david", players.toString())
            if(players.isNotEmpty()) {
                for(player in players) {
                    Log.d("Player log name", player.name)
                    Log.d("Player log level", player.level)
                }
            }
        })
        return root
    }

    fun addPlayer() {
        Realm.getDefaultInstance().use { realm ->
            val player = Player()
            // supply player name and level
            realm.executeTransaction { realm ->
                realm.insert(player)
            }
        }
    }
}
