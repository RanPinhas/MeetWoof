package com.example.meetwoof

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DogsAdapter(
    private val dogs: List<Dog>,
    private val currentUserId: String,
    private val onDogClick: (Dog) -> Unit,
    private val onDeleteClick: (Dog) -> Unit
) : RecyclerView.Adapter<DogsAdapter.DogViewHolder>() {

    class DogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgDog: ImageView = itemView.findViewById(R.id.imgDog)
        val tvName: TextView = itemView.findViewById(R.id.tvDogName)
        val tvBreed: TextView = itemView.findViewById(R.id.tvDogBreed)
        val btnViewReviews: ImageView = itemView.findViewById(R.id.btnViewReviews)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteDog)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEditDog)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dog_card, parent, false)
        return DogViewHolder(view)
    }

    override fun onBindViewHolder(holder: DogViewHolder, position: Int) {
        val dog = dogs[position]

        holder.tvName.text = dog.name
        holder.tvBreed.text = dog.breed

        if (dog.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(dog.imageUrl)
                .centerCrop()
                .into(holder.imgDog)
        } else {
            holder.imgDog.setImageResource(R.drawable.img_logo)
        }

        holder.itemView.setOnClickListener { onDogClick(dog) }
        holder.btnEdit.setOnClickListener { onDogClick(dog) }

        holder.btnViewReviews.setOnClickListener {
            val intent = Intent(holder.itemView.context, DogReviewsActivity::class.java)
            intent.putExtra("TARGET_DOG_ID", dog.id)
            intent.putExtra("TARGET_DOG_NAME", dog.name)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(dog)
        }
    }

    override fun getItemCount() = dogs.size
}