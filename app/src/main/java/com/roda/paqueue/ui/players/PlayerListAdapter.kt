package com.roda.paqueue.ui.players

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.textfield.TextInputLayout
import com.roda.paqueue.R
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import com.roda.paqueue.ui.queue.QueueConstants
import com.tubb.smrv.SwipeHorizontalMenuLayout
import io.realm.Realm
import io.realm.kotlin.where
import java.util.*

class PlayerListAdapter(context: Context?, onClickListener: OnClickListener, onEditListener: OnEditListener) : RecyclerView.Adapter<PlayerListAdapter.PlayerViewHolder>() {

    private val playerSortedList: SortedList<Player> = SortedList(Player::class.java, object : SortedListAdapterCallback<Player>(this) {
        override fun compare(p1: Player, p2: Player): Int = p1.name.toLowerCase(Locale.ROOT).compareTo(p2.name.toLowerCase(Locale.ROOT))

        override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem.id == newItem.id

        override fun areItemsTheSame(item1: Player, item2: Player): Boolean = item1 == item2
    })
    private var clickListener: OnClickListener = onClickListener
    private var editListener: OnEditListener = onEditListener
    private var mContext: Context? = null
    private var inputWasVisible: Boolean = false
    private var editHolder: PlayerViewHolder? = null

    init {
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_horizontal_menu_layout, parent, false)
        return PlayerViewHolder(view, clickListener)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.setPlayer(playerSortedList.get(position))

        holder.bind()

        if(playerSortedList.get(position).is_resting) {
            mContext?.getColor(R.color.blueBg)?.let { holder.itemView.setBackgroundColor(it) }
        }

        holder.btnDeletePlayer.setOnClickListener {
            val player = playerSortedList.get(holder.adapterPosition)
            removePlayer(player)
        }

        holder.imgBtnOptions.setOnClickListener {
            val player = playerSortedList.get(holder.adapterPosition)
            val popup = PopupMenu(it.context, it)
            popup.inflate(R.menu.player_row_menu)

            if (player.is_resting) {
                popup.menu.findItem(R.id.itmReadyPlayer).isVisible = true
                popup.menu.findItem(R.id.itmRestPlayer).isVisible = false
            } else {
                popup.menu.findItem(R.id.itmRestPlayer).isVisible = true
                popup.menu.findItem(R.id.itmReadyPlayer).isVisible = false
            }

            popup.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.itmEditPlayer -> {
                        holder.enableEdit(playerSortedList.get(holder.adapterPosition))
                        val layoutAddPlayer =
                            holder.itemView.rootView.findViewById<ConstraintLayout>(R.id.layoutAddPlayer)
                        val hideLayoutButton =
                            holder.itemView.rootView.findViewById<ImageButton>(R.id.imgBtnPlayerHideInput)
                        val showLayoutButton =
                            holder.itemView.rootView.findViewById<ImageButton>(R.id.imgBtnPlayerShowInput)
                        if (layoutAddPlayer.isVisible) {
                            inputWasVisible = true
                            layoutAddPlayer.visibility = View.GONE
                            hideLayoutButton.visibility = View.INVISIBLE
                            showLayoutButton.visibility = View.VISIBLE
                        }
                        editListener.onEditModeActive()
                        editHolder = holder
                    }
                    R.id.itmRestPlayer -> {
                        Realm.getDefaultInstance().use { realm ->
                            realm.executeTransaction {
                                player.is_resting = true
                            }
                        }
                        mContext?.getColor(R.color.blueBg)?.let { blue -> holder.itemView.setBackgroundColor(blue) }
                    }
                    R.id.itmReadyPlayer -> {
                        Realm.getDefaultInstance().use { realm ->
                            realm.executeTransaction {
                                player.is_resting = false
                            }
                        }
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
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
                holder.imgBtnOptions.visibility = View.VISIBLE
                holder.textViewPlayerGames.visibility = View.VISIBLE
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

                Toast.makeText(mContext, "$newPlayerName has been modified", Toast.LENGTH_SHORT).show()

                if (inputWasVisible) {
                    val layoutAddPlayer =
                        holder.itemView.rootView.findViewById<ConstraintLayout>(R.id.layoutAddPlayer)
                    val hideLayoutButton =
                        holder.itemView.rootView.findViewById<ImageButton>(R.id.imgBtnPlayerHideInput)
                    val showLayoutButton =
                        holder.itemView.rootView.findViewById<ImageButton>(R.id.imgBtnPlayerShowInput)
                    layoutAddPlayer.visibility = View.VISIBLE
                    hideLayoutButton.visibility = View.VISIBLE
                    showLayoutButton.visibility = View.INVISIBLE
                    inputWasVisible = false
                }
                editListener.onEditModeDone()
            }
        }
    }

    override fun onViewRecycled(holder: PlayerViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        holder.sml.closeEndMenuWithoutAnimation()
    }

    override fun getItemCount() = playerSortedList.size()

    fun getPlayer(position: Int): Player = playerSortedList.get(position)

    fun addPlayers(player: List<Player>) {
        playerSortedList.addAll(player)
    }

    fun removePlayer(player: Player?) {
        if (itemCount == 0) {
            return
        }
        Realm.getDefaultInstance().use { realm ->
            // check if player is in an active game before deleting
            val playerQueue = realm.where<Queue>()
                .equalTo("players.id", player?.id)
                .findFirst()
            if (playerQueue != null) {
                if (playerQueue.status == QueueConstants.STATUS_ACTIVE) {
                    Toast.makeText(
                        mContext,
                        player?.name + " is currently in-game",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                // delete queue object
                realm.executeTransaction {
                    playerQueue.deleteFromRealm()
                }
            }

            // delete player object
            playerSortedList.remove(player)
            realm.executeTransaction {
                player?.deleteFromRealm()
            }
        }
    }

    fun exitEditMode() {
        val player = editHolder?.adapterPosition?.let { playerSortedList.get(it) }
        editHolder?.textViewPlayerName?.visibility = View.VISIBLE
        editHolder?.imgBtnOptions?.visibility = View.VISIBLE
        editHolder?.textViewPlayerGames?.visibility = View.VISIBLE
        editHolder?.ratingBarLevel?.setIsIndicator(true)
        editHolder?.textInputLayout?.visibility = View.GONE
        editHolder?.imgBtnDoneEditing?.visibility = View.GONE

        // reset player level in case it was changed
        editHolder?.ratingBarLevel?.rating = player?.level!!

        if (inputWasVisible) {
            val layoutAddPlayer =
                editHolder?.itemView?.rootView?.findViewById<ConstraintLayout>(R.id.layoutAddPlayer)
            val hideLayoutButton =
                editHolder?.itemView?.rootView?.findViewById<ImageButton>(R.id.imgBtnPlayerHideInput)
            val showLayoutButton =
                editHolder?.itemView?.rootView?.findViewById<ImageButton>(R.id.imgBtnPlayerShowInput)
            layoutAddPlayer?.visibility = View.VISIBLE
            hideLayoutButton?.visibility = View.VISIBLE
            showLayoutButton?.visibility = View.INVISIBLE
            inputWasVisible = false
        }
        editListener.onEditModeDone()
    }

    private fun editPlayer(player: Player, index: Int) {
        playerSortedList.updateItemAt(index, player)
    }

    class PlayerViewHolder(itemView: View, private var onClickListener: OnClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        var textViewPlayerName: TextView = itemView.findViewById(R.id.textViewPlayerName)
        var textViewPlayerGames: TextView = itemView.findViewById(R.id.textViewPlayerGames)
        var ratingBarLevel: RatingBar = itemView.findViewById(R.id.ratingBarLevel)
        var btnDeletePlayer: ImageButton = itemView.findViewById(R.id.btnDeletePlayer)
        var imgBtnOptions: ImageButton = itemView.findViewById(R.id.imgBtnOptions)
        var imgBtnDoneEditing: ImageButton = itemView.findViewById(R.id.btnDoneEditing)
        var textInputLayout: TextInputLayout = itemView.findViewById(R.id.textInputLayout)
        var editTextEditPlayer: EditText = itemView.findViewById(R.id.editTextEditPlayerName)
        var sml: SwipeHorizontalMenuLayout = itemView.findViewById(R.id.sml)

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
            imgBtnOptions.visibility = View.GONE
            textViewPlayerGames.visibility = View.GONE
            ratingBarLevel.setIsIndicator(false)
            textInputLayout.visibility = View.VISIBLE
            imgBtnDoneEditing.visibility = View.VISIBLE
            editTextEditPlayer.setText(player.name)
        }
    }

    interface OnEditListener {
        fun onEditModeActive()
        fun onEditModeDone()
    }

    interface OnClickListener {
        fun onItemClick(position: Int, itemView: View?)
        fun onItemLongClick(position: Int, itemView: View?)
    }
}
