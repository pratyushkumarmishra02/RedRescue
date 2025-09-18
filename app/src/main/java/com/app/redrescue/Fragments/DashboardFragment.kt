package com.app.redrescue.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.app.redrescue.LoginActivity
import com.app.redrescue.MainActivity
import com.app.redrescue.R
import com.app.redrescue.SOS
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class DashboardFragment : Fragment() {

    // UI components
    private lateinit var settingsIcon: ImageView
    private lateinit var btnSos: MaterialButton
    private lateinit var multimediaEvidence: LinearLayout
    private lateinit var locationSharing: LinearLayout
    private lateinit var onlyDescription: LinearLayout
    private lateinit var anonymous: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileImageView: CircleImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var imgProfile: CircleImageView
    private lateinit var fuser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        fuser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        // Initialize views
        imgProfile = view.findViewById(R.id.imgProfile)
        settingsIcon = view.findViewById(R.id.settingsIcon)
        btnSos = view.findViewById(R.id.btnSos)
        multimediaEvidence = view.findViewById(R.id.multimediaEvidence)
        locationSharing = view.findViewById(R.id.locationSharing)
        onlyDescription  = view.findViewById(R.id.onlyDescription)
        anonymous = view.findViewById(R.id.anonymous)


        btnSos.setOnClickListener {
            startActivity(Intent(requireActivity(), SOS::class.java))
        }


        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Activity-level views (must be accessed after fragment's view is created)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout1)
        navigationView = requireActivity().findViewById(R.id.navigation_view1)

        val headerView = navigationView.getHeaderView(0)
        profileImageView = headerView.findViewById(R.id.nav_profile_image)
        userNameTextView = headerView.findViewById(R.id.textViewName)
        userEmailTextView = headerView.findViewById(R.id.textViewEmailEmail)

        imgProfile.setOnClickListener {
            (requireActivity() as MainActivity).toggleDrawer()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    //startActivity(Intent(requireContext(), ProfileActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    logOut()
                    true
                }
                else -> false
            }
        }

        loadDrawerHeader()
    }

    private fun loadDrawerHeader() {
        val userId: String = fuser.uid
        db.collection("USERS").document(userId).get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val profileImageUrl = documentSnapshot.getString("profilePicUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(profileImageUrl).placeholder(R.drawable.person)
                            .error(R.drawable.no_profile_pic).into(profileImageView)

                        Picasso.get().load(profileImageUrl).placeholder(R.drawable.person)
                            .error(R.drawable.no_profile_pic).into(imgProfile)
                    }

                    userNameTextView.text = documentSnapshot.getString("name") ?: ""
                    userEmailTextView.text = documentSnapshot.getString("email") ?: ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile details", Toast.LENGTH_SHORT).show()
            }
    }
    private fun logOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("LOGOUT")
            .setMessage("Do you want to Logout?")
            .setPositiveButton("YES") { _, _ ->
                firebaseAuth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
