package com.example.meetwoof

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewsAdapter(
    private val reviews: List<Review>,
    private val onLikeClick: (Review) -> Unit
) : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgReviewer: ImageView = view.findViewById(R.id.imgReviewer)
        val tvReviewerName: TextView = view.findViewById(R.id.tvReviewerName)
        val tvReviewDate: TextView = view.findViewById(R.id.tvReviewDate)
        val tvReviewContent: TextView = view.findViewById(R.id.tvReviewContent)
        val layoutLikeReview: LinearLayout = view.findViewById(R.id.layoutLikeReview)
        val btnLikeReview: ImageView = view.findViewById(R.id.btnLikeReview)
        val tvLikeCountReview: TextView = view.findViewById(R.id.tvLikeCountReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        holder.tvReviewerName.text = review.reviewerName
        holder.tvReviewContent.text = review.content
        holder.tvLikeCountReview.text = review.likedBy.size.toString()

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvReviewDate.text = sdf.format(Date(review.timestamp))

        if (review.reviewerImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(review.reviewerImageUrl).centerCrop().into(holder.imgReviewer)
        } else {
            holder.imgReviewer.setImageResource(R.drawable.img_person)
        }

        val isLikedByMe = review.likedBy.contains(currentUserId)

        if (isLikedByMe) {
            holder.btnLikeReview.imageTintList = ColorStateList.valueOf(Color.parseColor("#6d112a"))
        } else {
            holder.btnLikeReview.imageTintList = ColorStateList.valueOf(Color.parseColor("#886d112a"))
        }

        holder.layoutLikeReview.setOnClickListener {
            onLikeClick(review)
        }
    }

    override fun getItemCount() = reviews.size
}