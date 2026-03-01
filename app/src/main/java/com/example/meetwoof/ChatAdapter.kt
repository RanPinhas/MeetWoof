package com.example.meetwoof

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_SENT = 1
    private val TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByMe) TYPE_SENT else TYPE_RECEIVED
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTime: TextView = view.findViewById(R.id.tvMessageTime)
        val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_SENT) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is MessageViewHolder) {
            holder.tvContent.text = message.text
            holder.tvTime.text = message.time
            holder.tvSenderName.text = message.senderName
        }
    }

    override fun getItemCount() = messages.size
}