package com.roda.paqueue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.roda.paqueue.models.Player
import io.realm.Realm
import io.realm.kotlin.where

class SortedListAdapter : RecyclerView.Adapter<SortedListAdapter.UserViewHolder>() {

    private val playerSortedList: SortedList<Player>

    init {
        playerSortedList = SortedList(Player::class.java, object : SortedListAdapterCallback<Player>(this) {
            override fun compare(p1: Player, p2: Player): Int = p1.name.compareTo(p2.name)

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem.id == newItem.id

            override fun areItemsTheSame(item1: Player, item2: Player): Boolean = item1 == item2
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.horizontal_menu_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setPlayer(playerSortedList.get(position))

        holder.btnDeletePlayer.setOnClickListener {
            val player: Player = playerSortedList.get(holder.adapterPosition)
            removePlayer(holder.adapterPosition)
            Realm.getDefaultInstance().use { realm ->
                val playerDelete = realm.where<Player>().equalTo("id", player.id).findFirst()
                realm.executeTransaction {
                    playerDelete?.deleteFromRealm()
                }
            }
        }
    }

    override fun getItemCount() = playerSortedList.size()

    fun addPlayer(player: Player) {
        playerSortedList.add(player)
    }

    fun addPlayers(player: List<Player>) {
        playerSortedList.addAll(player)
    }

    private fun removePlayer(index: Int) {
        if (playerSortedList.size() == 0) {
            return
        }
        playerSortedList.remove(playerSortedList.get(index))
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textViewPlayerName: TextView = itemView.findViewById(R.id.textViewPlayerName)
        var ratingBarLevel: RatingBar = itemView.findViewById(R.id.ratingBarLevel)
        var btnDeletePlayer: ImageButton = itemView.findViewById(R.id.btnDeletePlayer)

        fun setPlayer(player: Player) {
            textViewPlayerName.text = player.name
            ratingBarLevel.rating = player.level
        }
    }
}