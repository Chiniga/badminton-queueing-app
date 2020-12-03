package com.roda.paqueue

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
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import com.roda.paqueue.ui.queue.QueueConstants
import com.tubb.smrv.SwipeHorizontalMenuLayout
import io.realm.Realm
import io.realm.kotlin.where
import java.util.*

class PlayerCostListAdapter(context: Context?, isAuto: Boolean) : RecyclerView.Adapter<PlayerCostListAdapter.PlayerCostViewHolder>() {

    private val playerSortedList: SortedList<Player>
    private var mContext: Context? = null
    private val mIsAuto = isAuto

    init {
        playerSortedList = SortedList(Player::class.java, object : SortedListAdapterCallback<Player>(this) {
            override fun compare(p1: Player, p2: Player): Int = p1.name.toLowerCase(Locale.ROOT).compareTo(p2.name.toLowerCase(
                Locale.ROOT))

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem.id == newItem.id

            override fun areItemsTheSame(item1: Player, item2: Player): Boolean = item1 == item2
        })
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerCostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_cost_row, parent, false)
        return PlayerCostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerCostViewHolder, position: Int) {
        holder.setPlayer(playerSortedList.get(position))

        holder.togglePaid.setOnCheckedChangeListener { _, isChecked ->
            val player = playerSortedList.get(holder.adapterPosition)

            Realm.getDefaultInstance().use { realm ->
                realm.executeTransaction {
                    player.is_paid = isChecked
                }
            }
        }

        if (mIsAuto) {
            holder.imageViewAdd.visibility = View.GONE
            holder.textViewNumBalls.visibility = View.GONE
            holder.imageViewSub.visibility = View.GONE
            holder.textViewPlayerGames.visibility = View.VISIBLE
        } else {
            holder.imageViewAdd.visibility = View.VISIBLE
            holder.textViewNumBalls.visibility = View.VISIBLE
            holder.imageViewSub.visibility = View.VISIBLE
            holder.textViewPlayerGames.visibility = View.GONE
        }

        holder.imageViewAdd.setOnClickListener {
            val player = playerSortedList.get(holder.adapterPosition)

            Realm.getDefaultInstance().use { realm ->
                realm.executeTransaction {
                    player.balls_used++
                    player.total_cost = player.balls_used * 5.00
                }
            }
            editPlayer(holder.adapterPosition, player)
        }

        holder.imageViewSub.setOnClickListener {
            val player = playerSortedList.get(holder.adapterPosition)

            if (player.balls_used > 0) {
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransaction {
                        player.balls_used--
                        player.total_cost = player.balls_used * 5.00
                    }
                }
                editPlayer(holder.adapterPosition, player)
            }
        }
    }

    override fun getItemCount() = playerSortedList.size()

    fun getPlayer(position: Int): Player = playerSortedList.get(position)

    fun addPlayers(player: List<Player>) {
        playerSortedList.addAll(player)
    }

    fun updatePlayerCost(players: List<Player>) {
        playerSortedList.clear()
        addPlayers(players)
    }

    private fun editPlayer(index: Int, player: Player) {
        playerSortedList.updateItemAt(index, player)
    }

    class PlayerCostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textViewPlayerName: TextView = itemView.findViewById(R.id.textViewPlayerName)
        var textViewPlayerGames: TextView = itemView.findViewById(R.id.textViewPlayerGames)
        var textViewTotalCost: TextView = itemView.findViewById(R.id.textViewTotalCost)
        var togglePaid: ToggleButton = itemView.findViewById(R.id.togglePaid)
        var imageViewSub: ImageView = itemView.findViewById(R.id.imgBtnSubNumBalls)
        var imageViewAdd: ImageView = itemView.findViewById(R.id.imgBtnAddNumBalls)
        var textViewNumBalls: TextView = itemView.findViewById(R.id.textViewNumBalls)

        fun setPlayer(player: Player) {
            textViewPlayerName.text = player.name
            textViewPlayerGames.text = player.num_games.toString()
            textViewTotalCost.text = player.total_cost.toString()
            togglePaid.isChecked = player.is_paid
            textViewNumBalls.text = player.balls_used.toString()
        }
    }
}