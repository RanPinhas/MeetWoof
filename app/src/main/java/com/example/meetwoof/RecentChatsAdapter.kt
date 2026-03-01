package com.example.meetwoof

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecentChatsAdapter(
    private val chats: List<ChatSummary>,
    private val onItemClick: (ChatSummary) -> Unit
) : RecyclerView.Adapter<RecentChatsAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChatName)
        val tvMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvChatTime)
        val imgProfile: ImageView = view.findViewById(R.id.imgChatProfile)
        val cvUnreadBadge: CardView = view.findViewById(R.id.cvUnreadBadge)
        val tvUnreadCount: TextView = view.findViewById(R.id.tvUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]

        holder.tvName.text = chat.name
        holder.tvMessage.text = chat.lastMessage
        holder.tvTime.text = chat.time

        if (chat.unreadCount > 0) {
            holder.cvUnreadBadge.visibility = View.VISIBLE
            holder.tvUnreadCount.text = chat.unreadCount.toString()
        } else {
            holder.cvUnreadBadge.visibility = View.GONE
        }

        if (chat.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(chat.imageUrl).centerCrop().into(holder.imgProfile)
        } else {
            holder.imgProfile.setImageResource(chat.imageRes)
        }

        holder.itemView.setOnClickListener {
            onItemClick(chat)
        }
    }

    override fun getItemCount() = chats.size
}