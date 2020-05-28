package com.roda.paqueue.ui.players

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.textfield.TextInputLayout
import com.roda.paqueue.R
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
            val player = playerSortedList.get(holder.adapterPosition)
            removePlayer(holder.adapterPosition)
            Realm.getDefaultInstance().use { realm ->
                val playerDelete = realm.where<Player>().equalTo("id", player.id).findFirst()
                realm.executeTransaction {
                    playerDelete?.deleteFromRealm()
                }
            }
        }

        holder.textViewOptions.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            popup.inflate(R.menu.player_row_menu)

            popup.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.itmEditPlayer ->
                        holder.enableEdit(playerSortedList.get(holder.adapterPosition))
                }
                true
            }

            popup.show()
        }

        holder.imgBtnDoneEditing.setOnClickListener {
            val currPos = holder.adapterPosition
            val player = playerSortedList.get(currPos)
            val newPlayerName = holder.editTextEditPlayer.text
            val newPlayerLevel = holder.ratingBarLevel.rating
            if(player.isValidName(newPlayerName.toString()) && newPlayerLevel != 0.0f) {
                holder.textViewPlayerName.visibility = View.VISIBLE
                holder.textViewOptions.visibility = View.VISIBLE
                holder.ratingBarLevel.setIsIndicator(true)
                holder.textInputLayout.visibility = View.GONE
                holder.imgBtnDoneEditing.visibility = View.GONE
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransaction { r ->
                        player.name = newPlayerName.toString()
                        player.level = newPlayerLevel
                        editPlayer(player, currPos)
                        r.insertOrUpdate(player)
                    }
                }

                val softKeyboard: InputMethodManager = holder.itemView.rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if(softKeyboard.isActive) {
                    softKeyboard.hideSoftInputFromWindow(holder.itemView.rootView.windowToken, 0)
                }
            }
        }
    }

    override fun getItemCount() = playerSortedList.size()

    fun addPlayers(player: List<Player>) {
        playerSortedList.addAll(player)
    }

    private fun editPlayer(player: Player, index: Int) {
        playerSortedList.updateItemAt(index, player)
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
        var textViewOptions: TextView = itemView.findViewById(R.id.textViewOptions)
        var imgBtnDoneEditing: ImageButton = itemView.findViewById(R.id.btnDoneEditing)
        var textInputLayout: TextInputLayout = itemView.findViewById(R.id.textInputLayout)
        var editTextEditPlayer: EditText = itemView.findViewById(R.id.editTextEditPlayerName)

        fun setPlayer(player: Player) {
            textViewPlayerName.text = player.name
            ratingBarLevel.rating = player.level
        }

        fun enableEdit(player: Player) {
            textViewPlayerName.visibility = View.GONE
            textViewOptions.visibility = View.GONE
            ratingBarLevel.setIsIndicator(false)
            textInputLayout.visibility = View.VISIBLE
            imgBtnDoneEditing.visibility = View.VISIBLE
            editTextEditPlayer.setText(player.name)
            editTextEditPlayer.requestFocus()

            val softKeyboard: InputMethodManager = itemView.rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            softKeyboard.showSoftInput(itemView.rootView, InputMethodManager.SHOW_FORCED)
        }
    }
}
