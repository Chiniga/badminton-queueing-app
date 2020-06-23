package com.roda.paqueue.ui.queue

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.roda.paqueue.R
import com.roda.paqueue.models.Player
import com.roda.paqueue.models.Queue
import com.tubb.smrv.SwipeHorizontalMenuLayout
import io.realm.Realm
import java.util.*

class ListAdapter(context: Context?, onClickListener: OnClickListener) : RecyclerView.Adapter<ListAdapter.UserViewHolder>() {

    private val queueList: ArrayList<Player> = ArrayList()
    private var listener: OnClickListener = onClickListener
    private var mContext: Context? = null

    init {
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.queue_row, parent, false)
        return UserViewHolder(mContext, view, listener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setQueue(queueList[position])

        holder.bind()
    }

    override fun getItemCount() = queueList.size

    fun addQueues(queues: List<Player>) {
        queueList.addAll(queues)
    }

    fun removeQueue(queue: Player?) {
        if (queueList.size == 0) {
            return
        }
        queueList.remove(queue)
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                queue?.deleteFromRealm()
            }
        }
    }

    class UserViewHolder(context: Context?, itemView: View, private var onClickListener: OnClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

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

        fun setQueue(queue: Player) {

        }
    }

    interface OnClickListener {
        fun onItemClick(position: Int, itemView: View?)
        fun onItemLongClick(position: Int, itemView: View?)
    }
}
