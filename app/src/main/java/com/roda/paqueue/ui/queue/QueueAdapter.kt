package com.roda.paqueue.ui.queue

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.roda.paqueue.R
import com.roda.paqueue.models.Court
import com.roda.paqueue.models.Queue
import com.roda.paqueue.ui.players.SortedListAdapter
import com.tubb.smrv.SwipeHorizontalMenuLayout
import io.realm.Realm
import io.realm.kotlin.where

class QueueAdapter(context: Context?, onClickListener: OnClickListener) : ListAdapter<Queue, QueueAdapter.UserViewHolder>(QueueListItemCallback()) {

    private var listener: OnClickListener = onClickListener
    private var mContext: Context? = null

    init {
        mContext = context
    }

    private class QueueListItemCallback : DiffUtil.ItemCallback<Queue>() {
        override fun areItemsTheSame(oldItem: Queue, newItem: Queue): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Queue, newItem: Queue): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.queue_horizontal_menu_layout, parent, false)
        return UserViewHolder(mContext, view, listener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setQueue(getItem(position))

        holder.bind()

        if(getItem(position).status == QueueConstants.STATUS_ACTIVE) {
            mContext?.let { context ->
                holder.layoutQueueItem.setBackgroundColor(ContextCompat.getColor(context, R.color.greenBg))
            }
            holder.finishQueue.visibility = View.VISIBLE
        }

        holder.finishQueue.setOnClickListener {
            finishQueue(getItem(position))
            Toast.makeText(mContext, "Game finished", Toast.LENGTH_SHORT).show()
        }

        holder.deleteQueue.setOnClickListener {
            removeQueue(getItem(position), false)
            Toast.makeText(mContext, "Game removed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewRecycled(holder: QueueAdapter.UserViewHolder) {
        super.onViewRecycled(holder)
        holder.layoutQueueItem.setBackgroundColor(Color.TRANSPARENT)
        holder.sml.closeEndMenuWithoutAnimation()
    }

    private fun removeQueue(queue: Queue, isFinished: Boolean) {
        if (itemCount == 0) {
            return
        }
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                if (!isFinished) {
                    // subtract queue_count
                    queue.players.forEach { player ->
                        val queuesGames = player.queues_games.split('_')
                        val subQueue = Integer.parseInt(queuesGames[0]) - 1
                        player.queue_count--
                        player.queues_games = subQueue.toString() + "_" + queuesGames[1]
                        player.queue.remove(queue)
                    }
                }
                if (queue.status == QueueConstants.STATUS_ACTIVE) {
                    // replace with IDLE queue item
                    assignNewActive(realm, queue.court_number)
                }
                queue.deleteFromRealm()
            }
        }
    }

    private fun assignNewActive(realm: Realm, court_number: Int) {
        // check ACTIVE queues VS number of courts
        val courts = realm.where<Court>().findFirst()!!
        val activeQueues = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_ACTIVE).count()
        // only set IDLE queue to ACTIVE if ACTIVE queues are <= the actual number of courts available
        if(activeQueues <= courts.courts) {
            val nextIdle = realm.where<Queue>().equalTo("status", QueueConstants.STATUS_IDLE)
                .sort("created_at").findFirst()
            // check players of idle queue
            // if player is in-game, find next IDLE queue
            nextIdle?.status = QueueConstants.STATUS_ACTIVE
            nextIdle?.court_number = court_number
        }
    }

    private fun finishQueue(queue: Queue) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                // add game to Player
                queue.players.forEach { player ->
                    val queuesGames = player.queues_games.split('_')
                    val addGame = Integer.parseInt(queuesGames[1]) + 1
                    player.num_games++
                    player.queues_games = queuesGames[0] + "_" + addGame.toString()
                    player.queue.remove(queue)
                }
            }
            removeQueue(queue, true)
        }
    }

    inner class UserViewHolder(context: Context?, itemView: View, private var onClickListener: OnClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        var playerOne: TextView = itemView.findViewById(R.id.playerOne)
        var playerTwo: TextView = itemView.findViewById(R.id.playerTwo)
        var playerThree: TextView = itemView.findViewById(R.id.playerThree)
        var playerFour: TextView = itemView.findViewById(R.id.playerFour)
        var ratingBarPlayerOne: RatingBar = itemView.findViewById(R.id.ratingBarPlayerOneLevel)
        var ratingBarPlayerTwo: RatingBar = itemView.findViewById(R.id.ratingBarPlayerTwoLevel)
        var ratingBarPlayerThree: RatingBar = itemView.findViewById(R.id.ratingBarPlayerThreeLevel)
        var ratingBarPlayerFour: RatingBar = itemView.findViewById(R.id.ratingBarPlayerFourLevel)
        var courtNumber: TextView = itemView.findViewById(R.id.courtNumber)
        var layoutQueueItem: ConstraintLayout = itemView.findViewById(R.id.layoutQueueItem)
        var finishQueue: ImageButton = itemView.findViewById(R.id.btnFinishQueue)
        var deleteQueue: ImageButton = itemView.findViewById(R.id.btnDeleteQueue)
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

        fun setQueue(queue: Queue) {
            playerOne.text = queue.players[0]?.name
            playerTwo.text = queue.players[1]?.name
            playerThree.text = queue.players[2]?.name
            playerFour.text = queue.players[3]?.name
            ratingBarPlayerOne.rating = queue.players[0]?.level!!
            ratingBarPlayerTwo.rating = queue.players[1]?.level!!
            ratingBarPlayerThree.rating = queue.players[2]?.level!!
            ratingBarPlayerFour.rating = queue.players[3]?.level!!
            courtNumber.text = if(queue.court_number != 99) queue.court_number.toString() else ""
        }
    }

    interface OnClickListener {
        fun onItemClick(position: Int, itemView: View?)
        fun onItemLongClick(position: Int, itemView: View?)
    }
}
