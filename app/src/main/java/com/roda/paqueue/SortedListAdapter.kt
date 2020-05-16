package com.roda.paqueue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import androidx.transition.Visibility
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.roda.paqueue.models.Player
import io.realm.Realm
import io.realm.RealmResults
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
                realm.executeTransaction { r ->
                    val playerDelete: RealmResults<Player> = r.where<Player>().equalTo("id", player.id).findAllAsync()
                    playerDelete.deleteAllFromRealm()
                }
            }
        }

        holder.btnEditPlayer.setOnClickListener {
            val player: Player = playerSortedList.get(holder.adapterPosition)
            holder.textViewPlayerName.visibility = View.GONE
            holder.btnDoneEdit.visibility = View.VISIBLE
            holder.textInputLayout.visibility = View.VISIBLE
            holder.ratingBarLevel.setIsIndicator(false)
            holder.editTextEditPlayerName.setText(player.name)
        }

        holder.btnDoneEdit.setOnClickListener {
            val currPos: Int = holder.adapterPosition
            val player: Player = playerSortedList.get(currPos)
            val newPlayerName = holder.editTextEditPlayerName.text
            val newPlayerLevel = holder.ratingBarLevel.rating
            if(player.isValidName(newPlayerName.toString()) && newPlayerLevel != 0.0f) {
                playerSortedList.clear()
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransaction { r ->
                        player.name = newPlayerName.toString()
                        player.level = newPlayerLevel
                        r.insertOrUpdate(player)
                    }
                }
            }
        }
    }

    override fun getItemCount() = playerSortedList.size()

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
        var btnEditPlayer: ImageButton = itemView.findViewById(R.id.btnEditPlayer)
        var textInputLayout: TextInputLayout = itemView.findViewById(R.id.textInputLayout)
        var editTextEditPlayerName: TextInputEditText = itemView.findViewById(R.id.editTextEditPlayerName)
        var btnDoneEdit: ImageButton = itemView.findViewById(R.id.btnDoneEditing)

        fun setPlayer(player: Player) {
            textViewPlayerName.text = player.name
            ratingBarLevel.rating = player.level
        }
    }
}