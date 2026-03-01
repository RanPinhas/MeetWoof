package com.example.meetwoof

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyReviewsAdapter(
    private val reviews: MutableList<Review>,
    private val onDeleteClick: (Review) -> Unit
) : RecyclerView.Adapter<MyReviewsAdapter.MyReviewViewHolder>() {

    class MyReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDogName: TextView = view.findViewById(R.id.tvTargetDogName)
        val tvContent: TextView = view.findViewById(R.id.tvMyReviewContent)
        val tvDate: TextView = view.findViewById(R.id.tvMyReviewDate)
        val tvLikes: TextView = view.findViewById(R.id.tvMyReviewLikes)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_review, parent, false)
        return MyReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyReviewViewHolder, position: Int) {
        val review = reviews[position]

        holder.tvDogName.text = review.targetDogName
        holder.tvContent.text = review.content

        holder.tvLikes.text = review.likedBy.size.toString()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(review.timestamp))

        holder.btnDelete.setOnClickListener {
            onDeleteClick(review)
        }
    }

    override fun getItemCount() = reviews.size

    fun removeItem(review: Review) {
        val position = reviews.indexOf(review)
        if (position != -1) {
            reviews.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}