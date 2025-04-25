package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityUserRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserRegister : AppCompatActivity() {

    private lateinit var binding: ActivityUserRegisterBinding
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var user: UserData
    private lateinit var userType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        userType = intent.getStringExtra("user") ?: "User  "
        binding.userDisplayName.text = "Create a $userType Account"

        // Restrict phone input to digits only
        binding.phone.filters = arrayOf(
            android.text.InputFilter.LengthFilter(11), // Limit input length to 11 digits
            android.text.InputFilter { source, _, _, _, _, _ ->
                if (source.matches(Regex("^[0-9]*$"))) {
                    source // Allow digits
                } else {
                    "" // Block non-digits
                }
            }
        )

        binding.btnSubmit.setOnClickListener {
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val email = binding.email.text.toString()
            val phone = binding.phone.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            // Show Terms and Conditions dialog before proceeding
            showTermsAndConditionsDialog(firstName, lastName, email, phone, password, confirmPassword)
        }
    }

    private fun showTermsAndConditionsDialog(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        // The Terms & Conditions text formatted with HTML
        val termsText = """
    <h2>Terms & Conditions</h2>
    <b>Effective Date: December 18, 2024</b><br><br>

    Welcome to SecureRide! These Terms and Conditions govern your access to and use of the SecureRide mobile application provided by BSIT students at Aces Tagum College, Inc. By downloading, accessing, or using the Application, you agree to these Terms. If you do not agree, please do not use the Application.
    <br><br>
    <b>1. Definitions</b><br>
     <b>• Application:</b> The tricycle booking platform provided by BSIT students at Aces Tagum College, Inc.<br>
     <b>• Commuter:</b> Any individual who registers or uses the Application to book a tricycle.<br>
     <b> • Driver:</b> A tricycle operator registered to provide transport services through the Application.<br>
    <b> • Service:</b> The facilitation of booking tricycle rides between Commuters and Drivers.<br>
    
     <b>2. Commuter Registration</b><br>
        • Upon opening the Application, Commuters are prompted to log in with existing credentials or register as a new user by providing necessary personal information.<br>
        • To use the Application, you must create an account by providing accurate and complete personal information.<br>
        • You are responsible for maintaining the confidentiality of your account credentials and for any activity under your account.<br>
       <b>3. Booking Services</b><br>
        • Once logged in, Commuters are directed to the home screen to access the main menu containing various features.<br>
        <b>• Booking Process:</b><br>
        1. Commuters can book nearby and recommend Drivers heading in the same direction through GPS technology.<br>
        2. Commuters may view Drivers' profiles before booking a ride.<br>
        3. Commuters must input pick-up and drop-off locations to request a ride. The estimated fare will be displayed before confirmation.<br>
        
        After the ride, Commuters can rate and provide feedback on their ride experience.<br>
        
         <b>4. Driver Services</b><br>
        • Drivers may pool multiple Commuters provided they have the same route, with a limitation on the number of Commuters per ride.<br>
        • If unable to accommodate a Commuter, Drivers can refer them to another available Driver.<br>
        • Drivers can view the total number of rides they have accommodated via updated reports.<br>
        <b>5. Fares and Payments</b><br>
        • Fares are calculated based on the distance traveled but with a fixed rate per kilometer.<br>
        • Payment is made directly to the Driver in cash or through any enabled in-app payment option.<br>
        • Once a ride is completed, <b>no refunds</b> will be issued.<br>
        
        <b>6. Real-Time Tracking</b><br>
        • The Application provides a real-time map feature that displays the live location of the Driver and Commuters on the shared ride.<br>
        • The map tracks the progress of the ride in real-time, showing the current location and estimated arrival time.<br>
        
        <b>7. Commuter Responsibilities</b><br>
        By using the Application, Commuters agree to:<br>
        • Treat Drivers with respect and courtesy.<br>
        • Follow local laws and regulations during rides.<br>
        • Use the Application for <b>lawful purposes only.</b><br>
        • Not cancel bookings excessively or abuse the booking system.<br>
        • After completing their tasks, Commuters can choose to exit the Application through the provided options.<br>
        
        <b>8. Driver Responsibilities</b><br>
        Registered Drivers agree to:<br>
        • Provide safe, professional, and timely transportation services.<br>
        • Ensure their tricycles are roadworthy and comply with local transportation laws.<br>
        • Maintain professional conduct with Commuters.<br>
        • Ensure fairness when pooling multiple Commuters.<br>
        
        <b>9. Cancellations and No-Shows</b><br>
        <b>• Driver Cancellations:</b> Drivers may cancel bookings if Commuters fail to show up within 10 minutes.<br>
        • Repeated cancellations or no-shows may result in account suspension.<br>
        
        <b>10. Limitation of Liability</b><br>
        We provide the Application as an intermediary platform only. We are <b>not responsible</b> for:<br>
        • Delays, cancellations, or issues caused by Drivers.<br>
        • Accidents, injuries, damages, or losses during rides.<br>
        • Technical glitches or interruptions in the Application.<br>
        • To the extent permitted by law, our liability is limited to the amount paid (if any) for the use of the Application.<br>
        
        <b>11. Privacy Policy</b><br>
        • Your use of the Application is subject to our <b>Privacy Policy</b>, which explains how we collect, use, and store your personal data. By using the Application, you agree to the collection and use of your data as outlined in the Privacy Policy.<br>
        
        <b>12. Prohibited Activities</b><br>
        Commuters and Drivers must not:<br>
        • Use the Application to commit fraud or unlawful activities.<br>
        • Harass, threaten, or harm Drivers or other Commuters.<br>
        • Tamper with the Application’s software or functionality.<br>
        
        <b>13. Account Suspension or Termination</b><br>
        We reserve the right to suspend or terminate your account without notice if you:<br>
        • Violate these Terms.<br>
        • Use the Application for fraudulent or illegal activities.<br>
        • Engage in behavior that threatens the safety of Drivers or other Commuters.<br>
        
        <b>14. Updates to Terms</b><br>
        We may update these Terms from time to time. Commuters will be notified of changes, and continued use of the Application constitutes acceptance of the updated Terms.<br>
        
        <b>15. Governing Law</b><br>
        These Terms are governed by the laws of the Philippines. Any disputes will be resolved in the courts of Tagum City, Philippines.<br>
        
        <b>16. Contact Us</b><br>
        For any questions, concerns, or feedback, please contact us at:<br>
        <b>• Email:</b> informationt316@gmail.com<br>
        <b>• Phone:</b> +63 939 179 8998<br>
        <b>• Address:</b> Aces Tagum College, Inc., Tagum City, Philippines<br>
        
        By using the SecureRide Application, you confirm that you have read, understood, and agree to these Terms and Conditions.<br>
        
        <b>Thank you for choosing SecureRide!</b><br>
        
    """.trimIndent()

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16) // Add padding to the LinearLayout
        }

        // Create a TextView for the terms
        val termsTextView = TextView(this).apply {
            text = Html.fromHtml(termsText, Html.FROM_HTML_MODE_LEGACY)
            gravity = Gravity.START // Align text to the left
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // Increased text size for readability
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12) // Set bottom margin for spacing between paragraphs
            }
        }

        // Add the TextView to the LinearLayout
        linearLayout.addView(termsTextView)

        // Create a ScrollView to wrap the LinearLayout (so the terms are scrollable)
        val scrollView = ScrollView(this).apply {
            setPadding(16, 16, 16, 16) // Add padding to the ScrollView
            addView(linearLayout)
        }

        // Create the dialog with the ScrollView containing the LinearLayout and TextView
        AlertDialog.Builder(this)
            .setTitle("Terms and Conditions") // Set a title for the dialog
            .setView(scrollView)  // Set the ScrollView with LinearLayout as the content of the dialog
            .setCancelable(false)
            .setPositiveButton("Accept") { dialog, _ ->
                binding.checkBox.isChecked = true  // Check the checkbox if accepted
                dialog.dismiss()
                // Proceed with registration
                registerUser (firstName, lastName, email, phone, password, confirmPassword)
            }
            .setNegativeButton("Decline") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "You must accept the Terms and Conditions to register.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }


    private fun registerUser(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        if (!binding.checkBox.isChecked) {
            Toast.makeText(this, "You must accept the Terms and Conditions to register.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPassword(password)) {
            binding.password.error = "Password must be at least 8 characters long, contain at least one uppercase letter, one number, and one special character."
            return
        }

        if (password != confirmPassword) {
            binding.confirmPassword.error = "Password does not match!"
            Toast.makeText(this@UserRegister, "Password did not match!", Toast.LENGTH_SHORT).show()
            return
        }

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            if (firstName.isEmpty()) binding.firstName.error = "Please enter your first name!"
            if (lastName.isEmpty()) binding.lastName.error = "Please enter your last name!"
            if (email.isEmpty()) binding.email.error = "Please enter your email!"
            if (phone.isEmpty()) binding.phone.error = "Please enter your phone!"
            if (password.isEmpty()) binding.password.error = "Please enter your password!"
            if (confirmPassword.isEmpty()) binding.confirmPassword.error = "Please confirm your password!"
            return
        }

        if (!phone.matches(Regex("^[0-9]{11}$"))) {
            binding.phone.error = "Please enter a valid 11-digit phone number"
            return
        }

        // Register user with Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = firebaseAuth.currentUser?.uid ?: ""
                if (userId.isNotEmpty()) {
                    firebaseDatabaseReference.child("user").child(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    Log.d("REGISTER", "$userType is already registered!")
                                    Toast.makeText(this@UserRegister, "$userType is already registered!", Toast.LENGTH_SHORT).show()
                                } else {
                                    user = UserData(
                                        uid = userId,
                                        email = email,
                                        firstname = firstName,
                                        lastname = lastName,
                                        phone = phone,
                                        password = password,
                                        userType = userType, // UserType: Commuter, Driver, Admin
                                        isVerified = true,
                                        profileImage = null,
                                        plateNumberImage = null,
                                        licenceImage = null,
                                        tricycleImage = null,
                                        rating = null,
                                        totalFare = 0
                                    )

                                    val databaseRef = firebaseDatabaseReference.child("user").child(userId)
                                    databaseRef.setValue(user).addOnCompleteListener { saveTask ->
                                        if (saveTask.isSuccessful) {
                                            firebaseAuth.currentUser?.sendEmailVerification()
                                                ?.addOnCompleteListener { verifyTask ->
                                                    if (verifyTask.isSuccessful) {
                                                        Log.d("REGISTER", "Verification email sent!")
                                                    } else {
                                                        Log.e("REGISTER", "Failed to send verification email: ${verifyTask.exception?.message}")
                                                    }
                                                }

                                            // Simulate delay and navigate based on userType (Admin or User)
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                binding.progressBar.visibility = View.GONE

                                                val intent = when (userType) {
                                                    "Commuter" -> {
                                                        Log.d("REGISTER", "Hi $firstName, you registered as a Commuter")
                                                        Toast.makeText(this@UserRegister, "Registered as User", Toast.LENGTH_LONG).show()
                                                        Intent(this@UserRegister, UserDashboard::class.java)
                                                    }
                                                    "Admin" -> {
                                                        Log.d("REGISTER", "Hi $firstName, you registered as an Admin")
                                                        Toast.makeText(this@UserRegister, "Registered as Admin", Toast.LENGTH_LONG).show()
                                                        Intent(this@UserRegister, AdminDashboard::class.java)
                                                    }
                                                    else -> {
                                                        Log.d("REGISTER", "Hi $firstName, you registered as a User (default)")
                                                        Toast.makeText(this@UserRegister, "Registered as User", Toast.LENGTH_LONG).show()
                                                        Intent(this@UserRegister, UserDashboard::class.java)
                                                    }
                                                }

                                                intent.putExtra("user", userType)
                                                startActivity(intent)
                                                finish()
                                            }, 3000) // 3000ms delay
                                        } else {
                                            Log.e("REGISTER", "Error saving user data: ${saveTask.exception?.message}")
                                            Toast.makeText(
                                                this@UserRegister,
                                                "Error saving user data: ${saveTask.exception?.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("REGISTER", "Database error: ${error.message}")
                                Toast.makeText(this@UserRegister, "Database error: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        })
                } else {
                    Log.e("REGISTER", "User ID is null or empty.")
                    Toast.makeText(this@UserRegister, "User ID is null or empty.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("REGISTER", "Registration failed: ${task.exception?.message}")
                Toast.makeText(this@UserRegister, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun isValidPassword(password: String): Boolean {
        // Regular expression to check password criteria
        val passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
        return password.matches(Regex(passwordPattern))
    }
}