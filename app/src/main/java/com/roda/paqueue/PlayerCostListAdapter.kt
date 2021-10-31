package com.roda.paqueue

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.roda.paqueue.models.Player
import com.roda.paqueue.ui.queue.QueueConstants
import io.realm.Realm
import java.util.*

class PlayerCostListAdapter(context: Context?, private var onCalcMethodChange: OnCalculationMethodChangeListener) : RecyclerView.Adapter<PlayerCostListAdapter.PlayerCostViewHolder>() {

    private val playerSortedList: SortedList<Player> = SortedList(Player::class.java, object : SortedListAdapterCallback<Player>(this) {
        override fun compare(p1: Player, p2: Player): Int = p1.name.toLowerCase(Locale.ROOT).compareTo(p2.name.toLowerCase(
            Locale.ROOT))

        override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem.id == newItem.id

        override fun areItemsTheSame(item1: Player, item2: Player): Boolean = item1 == item2
    })
    private var mContext: Context? = null
    private var pricePerBall: Double = 0.00

    init {
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerCostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_cost_row, parent, false)
        return PlayerCostViewHolder(view, onCalcMethodChange)
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

        holder.imageViewAdd.setOnClickListener {
            val player = playerSortedList.get(holder.adapterPosition)

            Realm.getDefaultInstance().use { realm ->
                realm.executeTransaction {
                    player.balls_used++
                    player.total_cost = player.balls_used * pricePerBall / QueueConstants.PLAYERS_PER_COURT
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
                        player.total_cost = player.balls_used * pricePerBall / QueueConstants.PLAYERS_PER_COURT
                    }
                }
                editPlayer(holder.adapterPosition, player)
            }
        }

        onCalcMethodChange.onMethodChange(holder)
    }

    override fun getItemCount() = playerSortedList.size()

    fun addPlayers(player: List<Player>) {
        playerSortedList.addAll(player)
    }

    fun updatePlayerCost(players: List<Player>) {
        playerSortedList.clear()
        addPlayers(players)
    }

    fun updatePricePerBall(price: Double) {
        pricePerBall = price
    }

    private fun editPlayer(index: Int, player: Player) {
        playerSortedList.updateItemAt(index, player)
    }

    class PlayerCostViewHolder(itemView: View, onCalcMethodChange: OnCalculationMethodChangeListener) : RecyclerView.ViewHolder(itemView) {

        var textViewPlayerName: TextView = itemView.findViewById(R.id.textViewPlayerName)
        var textViewPlayerGames: TextView = itemView.findViewById(R.id.textViewPlayerGames)
        var textViewTotalCost: TextView = itemView.findViewById(R.id.textViewTotalCost)
        var togglePaid: ToggleButton = itemView.findViewById(R.id.togglePaid)
        var imageViewSub: ImageView = itemView.findViewById(R.id.imgBtnSubNumBalls)
        var imageViewAdd: ImageView = itemView.findViewById(R.id.imgBtnAddNumBalls)
        var textViewNumBalls: TextView = itemView.findViewById(R.id.textViewNumBalls)

        init {
            onCalcMethodChange.onMethodChange(this)
        }

        fun setPlayer(player: Player) {
            textViewPlayerName.text = player.name
            textViewPlayerGames.text = player.num_games.toString()
            textViewTotalCost.text = player.total_cost.toString()
            togglePaid.isChecked = player.is_paid
            textViewNumBalls.text = player.balls_used.toString()
        }
    }

    interface OnCalculationMethodChangeListener {
        fun onMethodChange(viewHolder: PlayerCostViewHolder)
    }
}