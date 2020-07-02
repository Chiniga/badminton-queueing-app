package com.roda.paqueue.ui.players

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.textfield.TextInputLayout
import com.roda.paqueue.R
import com.roda.paqueue.models.Player
import com.tubb.smrv.SwipeHorizontalMenuLayout
import io.realm.Realm
import java.util.*

class SortedListAdapter(context: Context?, onClickListener: OnClickListener) : RecyclerView.Adapter<SortedListAdapter.UserViewHolder>() {

    private val playerSortedList: SortedList<Player>
    private var listener: OnClickListener
    private var mContext: Context? = null

    init {
        playerSortedList = SortedList(Player::class.java, object : SortedListAdapterCallback<Player>(this) {
            override fun compare(p1: Player, p2: Player): Int = p1.name.toLowerCase(Locale.ROOT).compareTo(p2.name.toLowerCase(Locale.ROOT))

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem.id == newItem.id

            override fun areItemsTheSame(item1: Player, item2: Player): Boolean = item1 == item2
        })
        listener = onClickListener
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_horizontal_menu_layout, parent, false)
        return UserViewHolder(mContext, view, listener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setPlayer(playerSortedList.get(position))

        holder.bind()

        holder.btnDeletePlayer.setOnClickListener {
            val player = playerSortedList.get(holder.adapterPosition)
            removePlayer(player)
            Toast.makeText(mContext, "Delete successful", Toast.LENGTH_LONG).show()
        }

        holder.textViewOptions.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            popup.inflate(R.menu.player_row_menu)

            popup.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.itmEditPlayer -> {
                        holder.enableEdit(playerSortedList.get(holder.adapterPosition))
                        val layoutAddPlayer =
                            holder.itemView.rootView.findViewById<ConstraintLayout>(R.id.layoutAddPlayer)
                        layoutAddPlayer.visibility = View.GONE
                    }
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
            if (player.isValid(mContext, newPlayerName.toString(), newPlayerLevel, player.id)) {
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

                Toast.makeText(mContext, "$newPlayerName has been modified", Toast.LENGTH_LONG).show()

                val softKeyboard: InputMethodManager = mContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (softKeyboard.isActive) {
                    softKeyboard.hideSoftInputFromWindow(holder.itemView.rootView.windowToken, 0)
                }

                val layoutAddPlayer =
                    holder.itemView.rootView.findViewById<ConstraintLayout>(R.id.layoutAddPlayer)
                layoutAddPlayer.visibility = View.VISIBLE

            }
        }
    }

    override fun onViewRecycled(holder: UserViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        holder.sml.closeEndMenuWithoutAnimation()
    }

    override fun getItemCount() = playerSortedList.size()

    fun getPlayer(position: Int): Player = playerSortedList.get(position)

    fun addPlayers(player: List<Player>) {
        playerSortedList.addAll(player)
    }

    private fun editPlayer(player: Player, index: Int) {
        playerSortedList.updateItemAt(index, player)
    }

    fun removePlayer(player: Player?) {
        if (playerSortedList.size() == 0) {
            return
        }
        playerSortedList.remove(player)
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                player?.deleteFromRealm()
            }
        }
    }

    class UserViewHolder(context: Context?, itemView: View, private var onClickListener: OnClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        var textViewPlayerName: TextView = itemView.findViewById(R.id.textViewPlayerName)
        var textViewPlayerGames: TextView = itemView.findViewById(R.id.textViewPlayerGames)
        var ratingBarLevel: RatingBar = itemView.findViewById(R.id.ratingBarLevel)
        var btnDeletePlayer: ImageButton = itemView.findViewById(R.id.btnDeletePlayer)
        var textViewOptions: TextView = itemView.findViewById(R.id.textViewOptions)
        var imgBtnDoneEditing: ImageButton = itemView.findViewById(R.id.btnDoneEditing)
        var textInputLayout: TextInputLayout = itemView.findViewById(R.id.textInputLayout)
        var editTextEditPlayer: EditText = itemView.findViewById(R.id.editTextEditPlayerName)
        var sml: SwipeHorizontalMenuLayout = itemView.findViewById(R.id.sml)

        private var mContext: Context? = context

        fun bind() {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            onClickListener.onItemClick(adapterPosition, v)
        }

        override fun onLongClick(v: View?): Boolean {
            onClickListener.onItemLongClick(adapterPosition, v)
            return true
        }

        fun setPlayer(player: Player) {
            textViewPlayerName.text = player.name
            textViewPlayerGames.text = player.num_games.toString()
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

            val softKeyboard: InputMethodManager = mContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            softKeyboard.showSoftInput(itemView.rootView, InputMethodManager.SHOW_FORCED)
        }
    }

    interface OnClickListener {
        fun onItemClick(position: Int, itemView: View?)
        fun onItemLongClick(position: Int, itemView: View?)
    }
}
