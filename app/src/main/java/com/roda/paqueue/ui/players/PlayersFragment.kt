package com.roda.paqueue.ui.players

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import PlayerModel
import com.roda.paqueue.R

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
            val players: List<PlayerModel> = it
            if(players.isNotEmpty()) {

            }
        })
        return root
    }
}
