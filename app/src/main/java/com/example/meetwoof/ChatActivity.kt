package com.example.meetwoof

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val messagesList = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter

    private var chatId: String = ""
    private var chatName: String = "Chat"
    private var targetDogId: String = ""
    private var targetDogName: String = ""
    private var myUserName: String = "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        chatName = intent.getStringExtra("CHAT_NAME") ?: "Chat"
        targetDogId = intent.getStringExtra("TARGET_DOG_ID") ?: ""
        targetDogName = intent.getStringExtra("TARGET_DOG_NAME") ?: ""

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Error: Chat ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvChatTitle).text = chatName
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        loadMyUserName()

        findViewById<ImageButton>(R.id.btnWriteReview).setOnClickListener {
            showAddReviewDialog()
        }

        val rvMessages = findViewById<RecyclerView>(R.id.rvChatMessages)
        adapter = ChatAdapter(messagesList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        listenForMessages()

        val etMessage = findViewById<EditText>(R.id.etMessage)
        findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                etMessage.text.clear()
            }
        }
    }

    private fun loadMyUserName() {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("users").document(myUid).get()
            .addOnSuccessListener { document ->
                myUserName = document.getString("name") ?: "Unknown"
            }
    }

    private fun listenForMessages() {
        val myUid = auth.currentUser?.uid ?: return

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    messagesList.clear()
                    for (doc in snapshots) {
                        val text = doc.getString("text") ?: ""
                        val senderId = doc.getString("senderId") ?: ""
                        val senderName = doc.getString("senderName") ?: "Unknown"
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                        val isMe = (senderId == myUid)

                        messagesList.add(Message(text, senderId, senderName, timestamp, isMe, timeString))
                    }
                    adapter.notifyDataSetChanged()

                    if (messagesList.isNotEmpty()) {
                        findViewById<RecyclerView>(R.id.rvChatMessages).scrollToPosition(messagesList.size - 1)
                    }
                }
            }
    }

    private fun sendMessage(text: String) {
        val myUid = auth.currentUser?.uid ?: return

        val messageData = hashMapOf(
            "text" to text,
            "senderId" to myUid,
            "senderName" to myUserName,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats").document(chatId).collection("messages").add(messageData)

        val chatUpdates = hashMapOf<String, Any>(
            "lastMessage" to text,
            "lastMessageTime" to System.currentTimeMillis()
        )
        db.collection("chats").document(chatId).update(chatUpdates)
    }

    private fun showAddReviewDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_review, null)
        dialog.setContentView(view)

        val etContent = view.findViewById<TextInputEditText>(R.id.etReviewContent)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitReview)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelReview)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val content = etContent.text.toString().trim()
            if (content.isNotEmpty()) {
                val newReviewRef = db.collection("reviews").document()
                val newReview = Review(
                    id = newReviewRef.id,
                    targetDogId = targetDogId,
                    targetDogName = targetDogName,
                    reviewerId = auth.currentUser?.uid ?: "",
                    reviewerName = myUserName,
                    reviewerImageUrl = "",
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    likedBy = emptyList()
                )

                newReviewRef.set(newReview).addOnSuccessListener {
                    Toast.makeText(this, "Review posted!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } else {
                etContent.error = "Please write something"
            }
        }
        dialog.show()
    }
}