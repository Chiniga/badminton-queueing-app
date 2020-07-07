package com.roda.paqueue.ui.queue

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.util.Log
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

class QueueListAdapter(context: Context?, onClickListener: OnClickListener) : RecyclerView.Adapter<QueueListAdapter.QueueViewHolder>() {

    private val queueSortedList: SortedList<Queue>
    private var listener: OnClickListener = onClickListener
    var mContext: Context? = null

    init {
        queueSortedList = SortedList(Queue::class.java, object : SortedListAdapterCallback<Queue>(this) {
            override fun compare(q1: Queue, q2: Queue): Int = q1.court_number.compareTo(q2.court_number)

            override fun areContentsTheSame(oldItem: Queue, newItem: Queue): Boolean = oldItem.hasSameContents(newItem)

            override fun areItemsTheSame(item1: Queue, item2: Queue): Boolean = item1 == item2
        })
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.queue_horizontal_menu_layout, parent, false)
        return QueueViewHolder(mContext, view, listener)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        holder.setQueue(queueSortedList.get(position))

        holder.bind()

        if(queueSortedList.get(holder.adapterPosition).status == QueueConstants.STATUS_ACTIVE) {
            mContext?.let { context ->
                holder.layoutQueueItem.setBackgroundColor(ContextCompat.getColor(context, R.color.greenBg))
            }
            holder.finishQueue.visibility = View.VISIBLE
        }

        holder.finishQueue.setOnClickListener {
            finishQueue(queueSortedList.get(holder.adapterPosition))
            Toast.makeText(mContext, "Game finished", Toast.LENGTH_SHORT).show()
        }

        holder.deleteQueue.setOnClickListener {
            removeQueue(queueSortedList.get(holder.adapterPosition), false)
            Toast.makeText(mContext, "Game removed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewRecycled(holder: QueueViewHolder) {
        super.onViewRecycled(holder)
        holder.layoutQueueItem.setBackgroundColor(Color.TRANSPARENT)
        holder.sml.closeEndMenuWithoutAnimation()
    }

    override fun getItemCount(): Int = queueSortedList.size()

    fun addQueues(queues: List<Queue>) {
        queueSortedList.addAll(queues)
    }

    private fun removeQueue(queue: Queue, isFinished: Boolean) {
        if (itemCount == 0) {
            return
        }
        queueSortedList.remove(queue)
        val queueStatus = queue.status
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                if (!isFinished) {
                    // subtract queue_count
                    queue.players.forEach { player ->
                        val queuesGames = player.queues_games.split('_')
                        val subQueue = Integer.parseInt(queuesGames[0]) - 1
                        player.queue_count--
                        player.queues_games = subQueue.toString() + "_" + queuesGames[1]
                        player.queues.remove(queue)
                    }
                }
                queue.deleteFromRealm()
            }
            if (queueStatus == QueueConstants.STATUS_ACTIVE) {
                // replace with IDLE queue item
                val queueManager = QueueManager(realm, mContext)
                val queues = queueManager.manageCourts()
                /*queues.forEach { q ->
                    Log.d(TAG, "removeQueue: $q")
                    val index = queueSortedList.indexOf(q)
                    Log.d(TAG, "removeQueue: $index")
                    queueSortedList.updateItemAt(index, q)
                }*/
            }
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