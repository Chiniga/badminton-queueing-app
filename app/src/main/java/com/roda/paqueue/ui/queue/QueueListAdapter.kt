package com.roda.paqueue.ui.queue

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.roda.paqueue.R
import com.roda.paqueue.models.Queue
import com.tubb.smrv.SwipeHorizontalMenuLayout
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import io.realm.kotlin.where

class QueueListAdapter(context: Context?, onClickListener: OnClickListener, queueList: RealmResults<Queue>) : RealmRecyclerViewAdapter<Queue, QueueListAdapter.QueueViewHolder>(queueList, true) {

    private var listener: OnClickListener = onClickListener
    var mContext: Context? = null

    init {
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.queue_horizontal_menu_layout, parent, false)
        return QueueViewHolder(mContext, view, listener)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        getItem(position)?.let { holder.setQueue(it) }

        holder.bind()

        if(getItem(position)?.status == QueueConstants.STATUS_ACTIVE) {
            mContext?.let { context ->
                holder.layoutQueueItem.setBackgroundColor(ContextCompat.getColor(context, R.color.greenBg))
            }
            holder.finishQueue.visibility = View.VISIBLE
        }

        holder.finishQueue.setOnClickListener {
            finishQueue(getItem(holder.adapterPosition))
            Toast.makeText(mContext, "Game finished", Toast.LENGTH_SHORT).show()
        }

        holder.deleteQueue.setOnClickListener {
            removeQueue(getItem(holder.adapterPosition), false)
            Toast.makeText(mContext, "Game removed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewRecycled(holder: QueueViewHolder) {
        super.onViewRecycled(holder)
        holder.layoutQueueItem.setBackgroundColor(Color.TRANSPARENT)
        holder.sml.closeEndMenuWithoutAnimation()
    }

    fun clearList() {
        Realm.getDefaultInstance().use { realm ->
            val queues = realm.where<Queue>().findAll()
            queues.forEach { queue ->
                removeQueue(queue, false)
            }
        }
    }

    private fun removeQueue(queue: Queue?, isFinished: Boolean) {
        if (itemCount == 0) {
            return
        }
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                if (!isFinished) {
                    // subtract queue_count
                    queue?.players?.forEach { player ->
                        player.queue_count--
                        player.queues_games = player.queues_games - 1
                        player.queues.remove(queue)
                    }
                }
                queue?.deleteFromRealm()
            }
            if (isFinished) {
                // replace with IDLE queue item
                val queueManager = QueueManager(realm, mContext)
                queueManager.manageCourts()
            }
        }
    }

    private fun finishQueue(queue: Queue?) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                // add game to Player
                queue?.players?.forEach { player ->
                    player.num_games++
                    player.queues_games = player.queues_games + 0.01f
                    player.queues.remove(queue)
                }
            }
            removeQueue(queue, true)
        }
    }

    class QueueViewHolder(context: Context?, itemView: View, private var onClickListener: OnClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
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
            playerOne.text = queue.players[0]!!.name
            playerTwo.text = queue.players[1]!!.name
            playerThree.text = queue.players[2]!!.name
            playerFour.text = queue.players[3]!!.name
            ratingBarPlayerOne.rating = queue.players[0]!!.level
            ratingBarPlayerTwo.rating = queue.players[1]!!.level
            ratingBarPlayerThree.rating = queue.players[2]!!.level
            ratingBarPlayerFour.rating = queue.players[3]!!.level
            courtNumber.text = if(queue.status == QueueConstants.STATUS_ACTIVE) queue.court_number.toString() else ""
        }
    }

    interface OnClickListener {
        fun onItemClick(position: Int, itemView: View?)
        fun onItemLongClick(position: Int, itemView: View?)
    }
}
