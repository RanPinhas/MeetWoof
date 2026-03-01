package com.example.meetwoof

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class RecentChatsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvChats: RecyclerView
    private lateinit var tvEmpty: TextView
    private val chatsList = mutableListOf<ChatSummary>()
    private lateinit var adapter: RecentChatsAdapter
    private var myDogId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent_chats)
        NavigationManager.setupBottomNavigation(this)

        rvChats = findViewById(R.id.rvRecentChats)
        tvEmpty = findViewById(R.id.tvEmptyChats)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnMyReviews).setOnClickListener {
            startActivity(Intent(this, MyReviewsActivity::class.java))
        }

        adapter = RecentChatsAdapter(chatsList) { chat ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.chatId)
            intent.putExtra("CHAT_NAME", chat.name)
            intent.putExtra("TARGET_DOG_ID", chat.id)
            intent.putExtra("TARGET_DOG_NAME", chat.targetDogName)
            startActivity(intent)
        }

        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = adapter

        loadMyChats()
    }

    private fun loadMyChats() {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("dogs").whereArrayContains("owners", myUid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    myDogId = documents.documents[0].id
                    listenForChats()
                } else {
                    tvEmpty.visibility = View.VISIBLE
                }
            }
    }

    private fun listenForChats() {
        val myUid = auth.currentUser?.uid ?: return

        db.collection("chats")
            .whereArrayContains("users", myUid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    chatsList.clear()
                    for (doc in snapshots) {
                        val participants = doc.get("participants") as? List<String> ?: continue
                        val users = doc.get("users") as? List<String> ?: continue

                        val otherDogId = participants.firstOrNull { it != myDogId } ?: participants.getOrNull(0) ?: ""
                        val otherUserId = users.firstOrNull { it != myUid } ?: users.getOrNull(0) ?: ""

                        val timeObj = doc.get("lastMessageTime")
                        val safeTime = when (timeObj) {
                            is Long -> timeObj
                            is Timestamp -> timeObj.toDate().time
                            is String -> timeObj.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        val chat = ChatSummary(
                            id = otherDogId,
                            chatId = doc.id,
                            name = "Loading...",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            time = convertTime(safeTime),
                            imageRes = R.drawable.img_logo,
                            imageUrl = "",
                            unreadCount = 0,
                            targetDogName = ""
                        )
                        chatsList.add(chat)
                        loadOtherDetails(otherDogId, otherUserId, chatsList.size - 1)
                    }
                    adapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (chatsList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun loadOtherDetails(dogId: String, userId: String, index: Int) {
        db.collection("dogs").document(dogId).get().addOnSuccessListener { dogDoc ->
            val dogName = dogDoc.getString("name") ?: "Unknown"
            val imageUrl = dogDoc.getString("imageUrl") ?: ""

            if (index < chatsList.size) {
                chatsList[index].targetDogName = dogName
            }

            if (userId.isNotEmpty()) {
                db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                    val userName = userDoc.getString("name") ?: "Owner"
                    if (index < chatsList.size) {
                        chatsList[index].name = "$userName & $dogName"
                        chatsList[index].imageUrl = imageUrl
                        adapter.notifyItemChanged(index)
                    }
                }
            } else {
                if (index < chatsList.size) {
                    chatsList[index].name = dogName
                    chatsList[index].imageUrl = imageUrl
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun convertTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}