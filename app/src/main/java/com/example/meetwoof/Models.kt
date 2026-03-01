package com.example.meetwoof


data class Dog(
    var id: String = "",
    val name: String = "",
    val age: Any = "0",
    val breed: String = "",
    val bio: String = "",
    val gender: String = "Male",
    val weight: String = "",
    val energyLevel: Int = 3,
    val imageUrl: String = "",
    val owners: MutableList<String> = mutableListOf(),
    val primaryOwnerId: String = "",
    val matches: MutableList<String> = mutableListOf()
)



data class ChatSummary(
    var id: String = "",
    var chatId: String = "",
    var name: String = "",
    var lastMessage: String = "",
    var time: String = "",
    var imageRes: Int = 0,
    var imageUrl: String = "",
    var unreadCount: Int = 0,
    var targetDogName: String = ""
)

data class Message(
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0,
    val isSentByMe: Boolean = false,
    val time: String = ""
)





data class Review(
    var id: String = "",
    var targetDogId: String = "",
    var targetDogName: String = "",
    var reviewerId: String = "",
    var reviewerName: String = "",
    var reviewerImageUrl: String = "",
    var content: String = "",
    var timestamp: Long = 0L,
    var likedBy: List<String> = emptyList()
)



data class Reminder(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var time: String = "",
    var date: String = "",
    var dogName: String = "",
    var timestamp: Long = 0L,
    var reminderTimestamp: Long = 0L,
    var owners: List<String> = emptyList(),
    var isDone: Boolean = false,
    var dogId: String = ""
)