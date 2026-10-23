package org.mehdiwelfare.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mehdi_welfare_local", MODE_PRIVATE) }
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var firebaseConfigured = false
    private var selectedBranch = "Nagar Khas"
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedBranch = prefs.getString("branch", "Nagar Khas") ?: "Nagar Khas"
        initFirebaseIfConfigured()
        if (firebaseConfigured && auth?.currentUser != null) showHome() else showLogin()
    }

    private fun initFirebaseIfConfigured() {
        try {
            // Replace these values using Firebase Console > Project settings > Your apps.
            // Do not publish service-account credentials in an Android app.
            val apiKey = "PASTE_FIREBASE_WEB_API_KEY_HERE"
            val appId = "PASTE_FIREBASE_APP_ID_HERE"
            val projectId = "PASTE_FIREBASE_PROJECT_ID_HERE"
            val senderId = "PASTE_FIREBASE_MESSAGING_SENDER_ID_HERE"
            if (apiKey.startsWith("PASTE_") || appId.startsWith("PASTE_") || projectId.startsWith("PASTE_") || senderId.startsWith("PASTE_")) {
                firebaseConfigured = false
                return
            }
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .setProjectId(projectId)
                .setGcmSenderId(senderId)
                .build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            firebaseConfigured = true
        } catch (_: Exception) {
            firebaseConfigured = false
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun startPage() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            setBackgroundColor(Color.rgb(247, 249, 247))
        }
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun showLogin() {
        startPage()
        text("MEHDI WELFARE ORGANIZATION", 22, "#0E5B43", true, Gravity.CENTER)
        text("Secure sign-in • Health • Education • Welfare", 14, "#4B6258", false, Gravity.CENTER)
        space(16)

        if (!firebaseConfigured) {
            card("Firebase setup required",
                "Login aur online database ko activate karne ke liye apna Firebase project banana aur neeche diye gaye setup guide ke mutabiq 4 configuration values dalna zaroori hai. Abhi online login/database connect nahi hai.")
            space(8)
        }

        val email = EditText(this).apply {
            hint = "Email address"
            inputType = 33
            singleLine = true
        }
        val password = EditText(this).apply {
            hint = "Password"
            inputType = 129
            singleLine = true
        }
        val branch = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Nagar Khas", "Karachi", "Central Admin"))
            setSelection(listOf("Nagar Khas", "Karachi", "Central Admin").indexOf(selectedBranch).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedBranch = listOf("Nagar Khas", "Karachi", "Central Admin")[position]
                    prefs.edit().putString("branch", selectedBranch).apply()
                }
            }
        }
        root.addView(email, LinearLayout.LayoutParams(-1, -2))
        root.addView(password, LinearLayout.LayoutParams(-1, -2))
        root.addView(branch, LinearLayout.LayoutParams(-1, -2))
        space(10)
        button("Log in") {
            if (!firebaseConfigured || auth == null) {
                message("Firebase setup required", "Pehle FIREBASE_SETUP.md follow karein. Setup ke baghair real login ya online database kaam nahi karega.")
            } else {
                val e = email.text.toString().trim()
                val p = password.text.toString()
                if (e.isBlank() || p.isBlank()) {
                    message("Missing information", "Email aur password dono enter karein.")
                } else {
                    auth!!.signInWithEmailAndPassword(e, p)
                        .addOnSuccessListener { showHome() }
                        .addOnFailureListener { message("Login failed", it.localizedMessage ?: "Email/password check karein.") }
                }
            }
        }
        button("Create first account") {
            if (!firebaseConfigured || auth == null) {
                message("Firebase setup required", "Pehle FIREBASE_SETUP.md complete karein.")
            } else {
                showCreateAccount()
            }
        }
        text("Privacy: beneficiary ke naam ya tasveer public na karein.", 13, "#4B6258", false, Gravity.CENTER)
    }

    private fun showCreateAccount() {
        val email = EditText(this).apply { hint = "Email address"; inputType = 33 }
        val password = EditText(this).apply { hint = "Password (at least 6 characters)"; inputType = 129 }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
            addView(email); addView(password)
        }
        AlertDialog.Builder(this).setTitle("Create login account")
            .setMessage("Account creation is for initial setup. Admin/branch roles must be secured using Firestore rules before real use.")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val e = email.text.toString().trim()
                val p = password.text.toString()
                if (e.isBlank() || p.length < 6) {
                    message("Check details", "Valid email aur kam az kam 6-character password enter karein.")
                } else {
                    auth!!.createUserWithEmailAndPassword(e, p)
                        .addOnSuccessListener {
                            val user = auth!!.currentUser
                            if (user != null) {
                                db!!.collection("users").document(user.uid)
                                    .set(mapOf("email" to e, "requestedBranch" to selectedBranch, "role" to "pending", "createdAt" to System.currentTimeMillis()))
                                    .addOnSuccessListener {
                                        message("Account created", "Account bana hai. Security ke liye role pending rakha gaya hai. Admin approval/rules configure hone tak real branch data use na karein.")
                                        auth!!.signOut()
                                        showLogin()
                                    }
                                    .addOnFailureListener { message("Database error", it.localizedMessage ?: "Firestore write failed.") }
                            }
                        }
                        .addOnFailureListener { message("Account creation failed", it.localizedMessage ?: "Try again.") }
                }
            }.show()
    }

    private fun showHome() {
        startPage()
        text("MEHDI WELFARE ORGANIZATION", 22, "#0E5B43", true, Gravity.CENTER)
        text("Health • Education • Family Support", 14, "#4B6258", false, Gravity.CENTER)
        space(10)
        text("Signed in: ${auth?.currentUser?.email ?: "Local preview"}", 13, "#4B6258", false, Gravity.CENTER)

        val branchSpinner = Spinner(this)
        val branches = listOf("Nagar Khas", "Karachi")
        branchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        branchSpinner.setSelection(branches.indexOf(selectedBranch).coerceAtLeast(0))
        branchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBranch = branches[position]
                prefs.edit().putString("branch", selectedBranch).apply()
            }
        }
        root.addView(branchSpinner)
        space(10)
        card("Privacy reminder", "Madad lene wale afrad ke naam aur tasveer public na karein. Online records ko real use se pehle secure Firestore rules aur role permissions se protect karna zaroori hai.")
        button("Add Donor") { showRecordForm("Donor") }
        button("Add Member / Volunteer") { showRecordForm("Member") }
        button("Program Spending Entry") { showRecordForm("Spending") }
        button("View Records") { showRecords() }
        button("Membership Fee Voucher") { showVoucherDialog() }
        button("Sync / Load online records") { loadOnlineRecords() }
        button("Log out") {
            auth?.signOut()
            showLogin()
        }
    }

    private fun text(value: String, size: Int, color: String, bold: Boolean, gravity: Int) {
        val tv = TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            this.gravity = gravity
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
        root.addView(tv, LinearLayout.LayoutParams(-1, -2))
    }

    private fun space(height: Int) { root.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }

    private fun card(title: String, body: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        box.addView(TextView(this).apply {
            text = title; textSize = 17f; setTextColor(Color.rgb(14, 91, 67))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        box.addView(TextView(this).apply {
            text = body; textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0, dp(5), 0, 0)
        })
        root.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
    }

    private fun button(label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label; isAllCaps = false; setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(14, 91, 67)); setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
    }

    private fun showRecordForm(kind: String) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), 0) }
        val name = EditText(this).apply { hint = if (kind == "Spending") "Program / item" else "Name / title"; singleLine = true }
        val contact = EditText(this).apply { hint = "Contact / WhatsApp (optional)"; inputType = 3; singleLine = true }
        val amount = EditText(this).apply { hint = "Amount in PKR (if applicable)"; inputType = 8194; singleLine = true }
        val program = Spinner(this)
        program.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Health", "Education", "Family support", "Wafat / condolences", "General"))
        layout.addView(name); layout.addView(contact); layout.addView(amount); layout.addView(program)
        AlertDialog.Builder(this).setTitle("Add $kind — $selectedBranch").setView(layout)
            .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                val record = hashMapOf<String, Any>(
                    "type" to kind, "branch" to selectedBranch,
                    "nameOrTitle" to name.text.toString().trim(),
                    "contact" to contact.text.toString().trim(),
                    "amount" to amount.text.toString().trim(),
                    "program" to program.selectedItem.toString(),
                    "createdAt" to System.currentTimeMillis(),
                    "createdBy" to (auth?.currentUser?.uid ?: "local-preview")
                )
                // Local copy is for convenience only; Firestore write is attempted when configured.
                val records = JSONArray(prefs.getString("records", "[]"))
                records.put(JSONObject(record))
                prefs.edit().putString("records", records.toString()).apply()
                if (firebaseConfigured && db != null && auth?.currentUser != null) {
                    db!!.collection("records").add(record)
                        .addOnSuccessListener { message("Saved", "Record is saved locally and uploaded online.") }
                        .addOnFailureListener { message("Saved locally only", "Online save failed: ${it.localizedMessage}") }
                } else message("Saved locally only", "Firebase configure nahi hai, is liye record is phone par hi save hua.")
            }.show()
    }

    private fun showRecords() {
        val records = JSONArray(prefs.getString("records", "[]"))
        val out = StringBuilder("Branch: $selectedBranch\n\n")
        var count = 0
        for (i in 0 until records.length()) {
            val item = records.optJSONObject(i) ?: continue
            if (item.optString("branch") != selectedBranch) continue
            count++
            out.append("$count. ${item.optString("type")} — ${item.optString("nameOrTitle")}\n")
            out.append("Program: ${item.optString("program")} | PKR: ${item.optString("amount")}\n\n")
        }
        if (count == 0) out.append("Is branch ke liye abhi koi local record nahi hai.")
        AlertDialog.Builder(this).setTitle("Local Records").setMessage(out.toString()).setPositiveButton("OK", null).show()
    }

    private fun loadOnlineRecords() {
        if (!firebaseConfigured || db == null || auth?.currentUser == null) {
            message("Not connected", "Firebase setup aur login ke baad online records load honge.")
            return
        }
        db!!.collection("records").whereEqualTo("branch", selectedBranch).get()
            .addOnSuccessListener { snapshot ->
                val out = StringBuilder("Online records for $selectedBranch: ${snapshot.size()}\n\n")
                snapshot.documents.take(30).forEachIndexed { i, doc ->
                    out.append("${i + 1}. ${doc.getString("type") ?: "Record"} — ${doc.getString("nameOrTitle") ?: ""}\n")
                    out.append("Program: ${doc.getString("program") ?: ""} | PKR: ${doc.getString("amount") ?: ""}\n\n")
                }
                message("Online records", out.toString())
            }
            .addOnFailureListener { message("Could not load", it.localizedMessage ?: "Check Firestore rules and connection.") }
    }

    private fun showVoucherDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), 0) }
        val member = EditText(this).apply { hint = "Member name"; singleLine = true }
        val contact = EditText(this).apply { hint = "WhatsApp number (optional)"; inputType = 3; singleLine = true }
        val amount = EditText(this).apply { hint = "Fee amount (PKR)"; inputType = 8194; singleLine = true }
        layout.addView(member); layout.addView(contact); layout.addView(amount)
        AlertDialog.Builder(this).setTitle("Membership Fee Voucher").setView(layout)
            .setNegativeButton("Cancel", null).setPositiveButton("Preview / Share") { _, _ ->
                val voucher = "MEHDI WELFARE ORGANIZATION\nMembership Fee Voucher\nBranch: $selectedBranch\nMember: ${member.text}\nContact: ${contact.text}\nFee: PKR ${amount.text}\n\nThank you for supporting welfare work."
                AlertDialog.Builder(this).setTitle("Voucher Preview").setMessage(voucher)
                    .setNeutralButton("Share / WhatsApp") { _, _ ->
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, voucher)
                        }
                        startActivity(android.content.Intent.createChooser(send, "Share voucher"))
                    }.setPositiveButton("OK", null).show()
            }.show()
    }

    private fun message(title: String, body: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show()
    }
}
