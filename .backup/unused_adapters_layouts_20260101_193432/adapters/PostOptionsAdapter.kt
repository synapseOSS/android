package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.PostActionItem

class PostOptionsAdapter(
    private val items: List<PostActionItem>
) : RecyclerView.Adapter<PostOptionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.optionIcon)
        private val label: TextView = itemView.findViewById(R.id.optionLabel)

        fun bind(item: PostActionItem) {
            label.text = item.label
            icon.setImageResource(item.icon)

            if (item.isDestructive) {
                val errorColor = ContextCompat.getColor(itemView.context, R.color.error_red)
                label.setTextColor(errorColor)
                icon.setColorFilter(errorColor)
            } else {
                val defaultColor = ContextCompat.getColor(itemView.context, R.color.text_primary)
                label.setTextColor(defaultColor)
                icon.clearColorFilter()
            }

            itemView.setOnClickListener { item.action() }
        }
    }
}
