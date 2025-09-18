package com.app.redrescue.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.redrescue.Domains.Contact
import com.app.redrescue.R

class ContactsAdapter(
    private var contacts: List<Contact>,
    private val onSelectChanged: (Contact, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactVH>() {

    inner class ContactVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.contactName)
        val phone: TextView = itemView.findViewById(R.id.contactPhone)
        val check: CheckBox = itemView.findViewById(R.id.contactCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactVH(v)
    }

    override fun onBindViewHolder(holder: ContactVH, position: Int) {
        val contact = contacts[position]

        holder.name.text = contact.name
        holder.phone.text = contact.phone

        // Remove old listener to prevent recycled view issues
        holder.check.setOnCheckedChangeListener(null)

        // Set checkbox state based on current contact selection
        holder.check.isChecked = contact.isSelected

        // Checkbox click → update contact and notify activity
        holder.check.setOnCheckedChangeListener { _, isChecked ->
            contact.isSelected = isChecked
            onSelectChanged(contact, isChecked)
        }

        // Row click → toggle checkbox and notify activity
        holder.itemView.setOnClickListener {
            val newChecked = !holder.check.isChecked
            holder.check.isChecked = newChecked
            contact.isSelected = newChecked
            onSelectChanged(contact, newChecked)
        }
    }

    override fun getItemCount() = contacts.size

    /**
     * Filters contacts based on a search query while keeping selection state.
     * @param query String search query
     * @param originalList Full contacts list to filter from
     */
    fun filterList(query: String, originalList: List<Contact>) {
        contacts = if (query.isBlank()) {
            originalList
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phone.contains(query)
            }
        }
        notifyDataSetChanged()
    }

    /**
     * Deprecated: May not be accurate after filtering.
     * Prefer managing selection outside adapter.
     */
    fun getSelectedContacts(): List<Contact> {
        return contacts.filter { it.isSelected }
    }
}
