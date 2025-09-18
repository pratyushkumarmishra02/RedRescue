package com.app.redrescue.Fragments

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
//import com.app.redrescue.FaqsActivity
//import com.app.redrescue.PersonalInformationActivity
import com.app.redrescue.R
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var profilePic: ImageView
    private lateinit var welcomeName: TextView
    private lateinit var personalInformation: LinearLayout
    private lateinit var notification: LinearLayout
    private lateinit var accountPrivacy: LinearLayout
    private lateinit var help: LinearLayout
    private lateinit var faqs: LinearLayout

    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views using view.findViewById
        profilePic = view.findViewById(R.id.profilePic)
        welcomeName = view.findViewById(R.id.welcomeName)
        personalInformation = view.findViewById(R.id.personalInformation)
        notification = view.findViewById(R.id.notification)
        accountPrivacy = view.findViewById(R.id.accountPrivacy)
        help = view.findViewById(R.id.helpSupport)
        faqs = view.findViewById(R.id.faqs)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        // Load user details
        loadUserDetails()

        // Handle clicks
        faqs.setOnClickListener {
           // startActivity(Intent(requireContext(), FaqsActivity::class.java))
        }


        personalInformation.setOnClickListener {
           // startActivity(Intent(requireContext(), PersonalInformationActivity::class.java))
        }

        return view
    }

    private fun loadUserDetails() {
        if (::firebaseUser.isInitialized) {
            val userId: String = firebaseUser.uid
            db.collection("USERS").document(userId)
                .get()
                .addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val name = documentSnapshot.getString("name")
                        val imageUrl = documentSnapshot.getString("profilePicUrl")

                        if (!name.isNullOrEmpty()) {
                            welcomeName.text = name
                        }
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.no_profile_pic)
                                .error(R.drawable.no_profile_pic)
                                .into(profilePic)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(ContentValues.TAG, "Failed to fetch user details: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to load profile details", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Sorry!! You are not an authenticated user", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserDetails()
    }
}
