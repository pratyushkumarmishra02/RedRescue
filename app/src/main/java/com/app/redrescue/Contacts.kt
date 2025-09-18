package com.app.redrescue

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.redrescue.Adapters.ContactsAdapter
import com.app.redrescue.Adapters.SelectedAdapter
import com.app.redrescue.Domains.Contact
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions



class Contacts : AppCompatActivity() {

    private lateinit var contactsRv: RecyclerView
    private lateinit var selectedRv: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var addMoreBtn: Button
    private lateinit var closeBtn: ImageView
    private lateinit var btnRefreshSelected: ImageView

    private val allContacts = mutableListOf<Contact>()

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var selectedAdapter: SelectedAdapter

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }

    private var trustedListener: ListenerRegistration? = null

    private val SMS_PERMISSION_REQUEST = 1001
    private var pendingSmsContacts: List<String>? = null
    private var pendingLat: Double? = null
    private var pendingLng: Double? = null



    //access to contact
    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("ContactsActivity", "Contacts permission granted: $granted")
            if (granted) loadDeviceContacts()
        }

    //access your current location
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Log.d("ContactsActivity", "Location permission granted: $it")
        }

    //access for sms
    private val requestSmsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted → you can now send SMS
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        contactsRv = findViewById(R.id.contactsRecyclerView)
        selectedRv = findViewById(R.id.selectedRecyclerView)
        searchInput = findViewById(R.id.searchInput)
        sendBtn = findViewById(R.id.btnSendSos)
        closeBtn = findViewById(R.id.btnClose1)
        saveBtn = findViewById(R.id.btnSaveContact)
        addMoreBtn = findViewById(R.id.btnAddMore)
        btnRefreshSelected = findViewById(R.id.btnRefreshSelected)


        btnRefreshSelected.setOnClickListener { updateSelectedListUI() }

        // Adapter for device contacts
        contactsAdapter = ContactsAdapter(allContacts) { contact, checked ->
            // Only update the isSelected state; DO NOT update selected UI immediately
            contact.isSelected = checked
        }

        // Adapter for selected contacts (vertical list)
        selectedAdapter = SelectedAdapter(mutableListOf()) { contact ->
            // Unselect in allContacts
            allContacts.find { it.phone == contact.phone }?.isSelected = false
            contactsAdapter.notifyDataSetChanged()

            removeContactFromFirestore(contact)
            updateSelectedListUI()
        }

        contactsRv.layoutManager = LinearLayoutManager(this)
        contactsRv.adapter = contactsAdapter

        selectedRv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        selectedRv.adapter = selectedAdapter
        selectedRv.visibility = View.GONE

        closeBtn.setOnClickListener { finish() }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                contactsAdapter.filterList(query, allContacts)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        saveBtn.setOnClickListener {
            val selected = getSelected()
            if (selected.isEmpty()) {
                Toast.makeText(this, "No contacts selected to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Merge with existing selectedAdapter items to avoid duplicates
            val currentSelected = selectedAdapter.getItems().toMutableList()
            val merged = (currentSelected + selected)
                .distinctBy { it.phone } // avoid duplicates based on phone

            selectedAdapter.setItems(merged)
            selectedRv.visibility = View.VISIBLE
            contactsRv.visibility = View.GONE
            saveBtn.visibility = View.GONE
            searchInput.visibility = View.GONE
            addMoreBtn.visibility = View.VISIBLE
            sendBtn.visibility = View.VISIBLE

            saveTrustedToFirestore(merged) {
                Toast.makeText(this, "Contacts saved successfully", Toast.LENGTH_SHORT).show()
            }
        }


        addMoreBtn.setOnClickListener {
            contactsRv.visibility = View.VISIBLE
            saveBtn.visibility = View.VISIBLE
            searchInput.visibility = View.VISIBLE
            selectedRv.visibility = View.GONE
            addMoreBtn.visibility = View.GONE
            sendBtn.visibility = View.GONE
            contactsAdapter.filterList("", allContacts)
        }

        sendBtn.setOnClickListener {
            val selected = getSelected()
            if (selected.isEmpty()) {
                Toast.makeText(this, "No contacts selected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sending SOS…", Toast.LENGTH_SHORT).show()
                fetchTokensAndSend(selected)
            }
        }

        ensurePermissionsAndLoad()
    }

    private fun ensurePermissionsAndLoad() {
        // Contacts permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        } else {
            loadDeviceContacts()
        }

        // Location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // SMS permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestSmsPermission.launch(Manifest.permission.SEND_SMS)
        }

        // Default SMS app check (Android 14/15 requirement)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
            if (defaultSmsPackage != packageName) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivity(intent) // Prompts user once
            }
        }

        // Load Firestore trusted contacts
        loadTrustedFromFirestore()
    }


    private fun loadTrustedFromFirestore() {
        val uid = auth.currentUser?.uid ?: return

        trustedListener?.remove()

        trustedListener = db.collection("Users")
            .document(uid)
            .collection("trusted_contacts")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e("Firestore", "Error listening trusted_contacts", e)
                    return@addSnapshotListener
                }

                if (snap != null) {
                    // Reset all selections first
                    allContacts.forEach { c -> c.isSelected = false }

                    val trustedPhones = snap.documents.mapNotNull { d -> d.getString("phone") }
                        .filter { it.isNotBlank() }

                    // Mark device contacts as selected if they are trusted
                    allContacts.forEach { contact ->
                        if (trustedPhones.contains(contact.phone)) {
                            contact.isSelected = true
                        }
                    }

                    // Update selected adapter with trusted contacts
                    val trustedContacts = allContacts.filter { it.isSelected }
                    if (trustedContacts.isNotEmpty()) {
                        selectedAdapter.setItems(trustedContacts.toMutableList())
                        selectedRv.visibility = View.VISIBLE
                        sendBtn.visibility = View.VISIBLE
                        addMoreBtn.visibility = View.VISIBLE

                        // Hide device contacts RecyclerView and save/search
                        contactsRv.visibility = View.GONE
                        saveBtn.visibility = View.GONE
                        searchInput.visibility = View.GONE
                    }
                }
            }
    }


    private fun loadDeviceContacts() {
        allContacts.clear()
        val seenPhones = mutableSetOf<String>() // To keep track of added phone numbers for redundancy

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idxName = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val idxPhone = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(idxName) ?: ""
                val rawPhone = it.getString(idxPhone) ?: ""

                // Normalize → keep only digits, then take last 10
                val digits = rawPhone.replace("\\D".toRegex(), "")
                val phone = if (digits.length > 10) digits.takeLast(10) else digits

                if (phone.isNotBlank() && !seenPhones.contains(phone)) {
                    allContacts.add(Contact(name, phone))
                    seenPhones.add(phone)
                }
            }
        }
        contactsAdapter.filterList("", allContacts)
    }


    private fun updateSelectedListUI() {
        val selected = getSelected()
        selectedAdapter.setItems(selected.toMutableList())
        selectedRv.visibility = if (selected.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun getSelected(): List<Contact> = allContacts.filter { it.isSelected }

    private fun fetchTokensAndSend(selectedList: List<Contact>) {
        if (selectedList.isEmpty()) return

        val tokens = mutableListOf<String>()
        val nonRegisteredPhones = mutableListOf<String>()

        // Normalize all phone numbers to last 10 digits
        val normalizedSelected = selectedList.map { contact ->
            val digits = contact.phone.replace("\\D".toRegex(), "")
            val phone = if (digits.length > 10) digits.takeLast(10) else digits
            contact.copy(phone = phone) // copy with normalized phone
        }

        val chunks = normalizedSelected.map { it.phone }.chunked(10)
        var completed = 0

        chunks.forEach { chunk ->
            db.collection("USERS").whereIn("phone", chunk)
                .get()
                .addOnSuccessListener { snap ->
                    val registeredPhones = mutableSetOf<String>()

                    snap.documents.forEach { doc ->
                        val phone = doc.getString("phone") ?: return@forEach
                        val fcm = doc.getString("fcmToken")
                        normalizedSelected.find { it.phone == phone }?.fcmToken = fcm
                        if (!fcm.isNullOrBlank()) tokens.add(fcm)
                        registeredPhones.add(phone)
                    }

                    // Phones not found in USERS are non-registered → fallback SMS
                    chunk.forEach { phone ->
                        if (!registeredPhones.contains(phone)) nonRegisteredPhones.add(phone)
                    }

                    completed++
                    if (completed == chunks.size) {
                        sendSosWithTokensAndSms(normalizedSelected, tokens, nonRegisteredPhones)
                    }
                }
                .addOnFailureListener {
                    nonRegisteredPhones.addAll(chunk)
                    completed++
                    if (completed == chunks.size) {
                        sendSosWithTokensAndSms(normalizedSelected, tokens, nonRegisteredPhones)
                    }
                }
        }
    }

    private fun sendSosWithTokensAndSms(
        selectedList: List<Contact>,
        tokens: List<String>,
        nonRegisteredPhones: List<String>
    ) {
        if (selectedList.isEmpty()) return

        saveTrustedToFirestore(selectedList) {
            val fused = LocationServices.getFusedLocationProviderClient(this)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // No location permission → send without location
                callCloudFunction(tokens, null, null)
                sendSmsToContacts(nonRegisteredPhones, null, null)
            } else {
                fused.lastLocation.addOnSuccessListener { loc ->
                    val lat = loc?.latitude
                    val lng = loc?.longitude
                    callCloudFunction(tokens, lat, lng)
                    sendSmsToContacts(nonRegisteredPhones, lat, lng)
                }.addOnFailureListener {
                    callCloudFunction(tokens, null, null)
                    sendSmsToContacts(nonRegisteredPhones, null, null)
                }
            }
        }
    }

    private fun sendSmsToContacts(phones: List<String>, lat: Double?, lng: Double?) {
        val mapsLink = if (lat != null && lng != null) {
            "https://maps.google.com/?q=$lat,$lng"
        } else {
            "Location not available"
        }
        val message = "🚨 SOS! I need help. My location: $mapsLink"

        // Save for retry if permission not granted
        pendingSmsContacts = phones
        pendingLat = lat
        pendingLng = lng

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST
            )
            return
        }

        try {
            if (isDefaultSmsApp()) {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val subId = SmsManager.getDefaultSmsSubscriptionId()
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                } else {
                    SmsManager.getDefault()
                }

                for (phone in phones) {
                    smsManager.sendTextMessage(phone, null, message, null, null)
                    Log.d("SMS", "Sent SMS silently to $phone")
                }

            } else {
                // Not default SMS app → use system Messages app
                val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("smsto:" + phones.joinToString(";"))
                    putExtra("sms_body", message)
                }
                startActivity(smsIntent)
                Log.d("SMS", "Opened system SMS app for sending")
            }

        } catch (e: Exception) {
            Log.e("SMS", "Failed to send SMS", e)
        }

        // Clear after sending
        pendingSmsContacts = null
        pendingLat = null
        pendingLng = null
    }

    // Helper to check if this app is default SMS app
    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
            packageName == defaultSmsPackage
        } else {
            true // old versions don’t care
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SMS", "SEND_SMS permission granted, retrying SMS")
                val contacts = pendingSmsContacts
                if (contacts != null) {
                    sendSmsToContacts(contacts, pendingLat, pendingLng)
                }
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun saveTrustedToFirestore(list: List<Contact>, onDone: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return onDone()
        val batch = db.batch()
        val col = db.collection("Users").document(uid).collection("trusted_contacts")

        list.forEach { c ->
            // Normalize phone → keep only digits and last 10
            val digits = c.phone.replace("\\D".toRegex(), "")
            val phone = if (digits.length > 10) digits.takeLast(10) else digits

            val doc = col.document(phone)
            val data = hashMapOf(
                "name" to c.name,
                "phone" to phone,
                "fcmToken" to c.fcmToken
            )
            batch.set(doc, data)
        }

        batch.commit().addOnCompleteListener { onDone() }
    }


    private fun removeContactFromFirestore(contact: Contact) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid)
            .collection("trusted_contacts")
            .document(contact.phone)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Removed ${contact.name} from trusted contacts")
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to remove ${contact.name}", it)
            }
    }

    private fun callCloudFunction(phoneNumbers: List<String>, lat: Double?, lng: Double?) {
        val data = hashMapOf(
            "phoneNumbers" to phoneNumbers,
            "latitude" to (lat ?: 0.0),
            "longitude" to (lng ?: 0.0)
        )

        val functions = Firebase.functions
        functions.getHttpsCallable("sendSOS")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<*, *>
                val success = response?.get("success") as? Boolean ?: false
                if (success) {
                    Log.d("SOS", "SOS sent successfully!")
                } else {
                    Log.e("SOS", "Function error: ${response?.get("error")}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("SOS", "Failed to call function: ${e.message}")
            }
    }



}
