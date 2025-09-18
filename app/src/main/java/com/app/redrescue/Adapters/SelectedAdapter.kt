package com.app.redrescue.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.redrescue.Domains.Contact
import com.app.redrescue.R

class SelectedAdapter(
    private val selectedList: MutableList<Contact>,
    private val onRemove: (Contact) -> Unit
) : RecyclerView.Adapter<SelectedAdapter.SelVH>() {

    inner class SelVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.selectedName)
        val phone: TextView = itemView.findViewById(R.id.selectedPhone)
        val remove: ImageView = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_contact, parent, false)
        return SelVH(v)
    }

    override fun onBindViewHolder(holder: SelVH, position: Int) {
        val c = selectedList[position]
        holder.name.text = c.name
        holder.phone.text = c.phone
        holder.remove.setOnClickListener {
            onRemove(c)
        }
    }

    override fun getItemCount() = selectedList.size

    fun setItems(newItems: List<Contact>) {
        selectedList.clear()
        selectedList.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<Contact> = selectedList.toList()
}
