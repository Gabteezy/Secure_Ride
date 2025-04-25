package com.aces.capstone.secureride

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.aces.capstone.secureride.databinding.ActivityDriverRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class DriverRegister : AppCompatActivity() {

    private lateinit var binding: ActivityDriverRegisterBinding
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var user: UserData
    private var driverPhotoBitmap: Bitmap? = null
    private lateinit var progressDialog: AlertDialog
    private lateinit var userType: String

    companion object {
        const val IMAGE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userType = intent.getStringExtra("user") ?: "User "
        binding.userDisplayName.text = "Create a $userType Account"

        // Restrict phone input to digits only
        binding.phone.filters = arrayOf(
            android.text.InputFilter.LengthFilter(11),
            android.text.InputFilter { source, _, _, _, _, _ ->
                if (source.matches(Regex("^[0-9]*$"))) {
                    source
                } else {
                    ""
                }
            }
        )

        binding.btnSubmit.setOnClickListener {
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val email = binding.email.text.toString()
            val phone = binding.phone.text.toString()
            val address = binding.address.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            // Show Terms and Conditions dialog before proceeding
            showTermsAndConditionsDialog(firstName, lastName, email, phone, address, password, confirmPassword)
        }

        binding.tricycleAdd.setOnClickListener { addTricycle() }
        binding.plateNumberAdd.setOnClickListener { addPlateNumber() }
        binding.driverAdd.setOnClickListener { addDriverPhoto() }
        binding.driversLicenseAdd.setOnClickListener { addDriversLicense() }
    }

    private fun showTermsAndConditionsDialog(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        address: String,
        password: String,
        confirmPassword: String
    ){
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
                registerUser (firstName, lastName, email, phone, address, password, confirmPassword)
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
        address: String,
        password: String,
        confirmPassword: String
    ) {
        if (!binding.checkBox.isChecked) {
            Toast.makeText(this, "You must accept the Terms and Conditions to register.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate input fields
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 8 characters long, contain at least one uppercase letter, one number, and one special character.", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog()

        // Create Firebase Authentication account
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = firebaseAuth.currentUser?.uid ?: ""
                Log.d("DriverRegister", "Firebase Authentication account created successfully. UID: $userId, Email: $email")

                if (userId.isNotEmpty()) {
                    // Save user data to Firebase Realtime Database
                    val user = UserData(
                        uid = userId,
                        email = email, // Ensure the email is stored
                        firstname = firstName,
                        lastname = lastName,
                        phone = phone,
                        address = address,
                        password = password,
                        userType = "Driver",
                        isVerified = false,
                        profileImage = null,
                        plateNumberImage = null,
                        licenceImage = null,
                        tricycleImage = null,
                        rating = null,
                        isOnline = true,
                        note = ""
                    )

                    val databaseRef = firebaseDatabaseReference.child("pending_drivers").child(userId)
                    databaseRef.setValue(user).addOnCompleteListener { saveTask ->
                        if (saveTask.isSuccessful) {
                            Log.d("DriverRegister", "User data saved to Realtime Database.")
                            saveImagesAsBase64(userId)
                            checkApprovalStatus(userId)
                        } else {
                            // Delete Firebase Authentication account if saving to Realtime Database fails
                            firebaseAuth.currentUser?.delete()?.addOnCompleteListener {
                                Log.e("DriverRegister", "Failed to save user data. Firebase Authentication account deleted.")
                                progressDialog.dismiss()
                                Toast.makeText(this, "Error saving user data. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Log.e("DriverRegister", "Firebase Authentication account creation failed: ${task.exception?.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val userId = firebaseAuth.currentUser?.uid ?: return
        val pendingRef = firebaseDatabaseReference.child("pending_drivers").child(userId)

        pendingRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot.exists()) {
                    val userData = snapshot.getValue(UserData::class.java)
                    if (userData != null && !userData.isVerified) {
                        // Delete the account only if the driver is still pending and not verified
                        firebaseAuth.currentUser?.delete()?.addOnCompleteListener {
                            Log.d("DriverRegister", "Firebase Authentication account deleted on app exit (pending driver).")
                        }
                    }
                }
            }
        }
    }

    private fun checkApprovalStatus(userId: String) {
        // First check in the approved drivers node
        val approvedUserRef = firebaseDatabaseReference.child("user").child(userId)

        approvedUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userData = snapshot.getValue(UserData::class.java)
                    if (userData != null) {
                        if (userData.isVerified) {
                            // User is approved, dismiss the progress dialog and navigate to the dashboard
                            progressDialog.dismiss()
                            navigateToDashboard()
                        }
                    }
                } else {
                    // If no data in approved_drivers, check in pending_drivers
                    val pendingUserRef = firebaseDatabaseReference.child("pending_drivers").child(userId)

                    // Listen for real-time updates
                    pendingUserRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val userData = snapshot.getValue(UserData::class.java)
                                if (userData != null) {
                                    if (userData.isVerified) {
                                        progressDialog.dismiss()
                                        navigateToDashboard()
                                    } else {
                                        // Check for admin note
                                        val adminNote = userData.note // Ensure this is correct
                                        if (adminNote != null && adminNote.isNotEmpty()) {
                                            progressDialog.dismiss()
                                            showAdminNoteDialog(adminNote)
                                        } else {
                                            // No admin note yet, keep the progress dialog visible
                                            Toast.makeText(this@DriverRegister, "Your registration is pending approval.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {

                                // No data found for user
                                progressDialog.dismiss()
                                Toast.makeText(this@DriverRegister, "User data not found.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error
                            progressDialog.dismiss()
                            Log.e("DriverRegister", "Error checking user data: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                progressDialog.dismiss()
                Log.e("DriverRegister", "Error checking approval status: ${error.message}")
            }
        })
    }






    private fun showAdminNoteDialog(adminNote: String) {
        progressDialog.dismiss()
        AlertDialog.Builder(this)
            .setTitle("Admin Note")
            .setMessage(adminNote)
            .setPositiveButton("OK") { dialog, _ ->
                // Delete Firebase Authentication account when the user acknowledges the admin note
                firebaseAuth.currentUser?.delete()?.addOnCompleteListener {
                    dialog.dismiss()
                    Toast.makeText(this, "Registration canceled.", Toast.LENGTH_SHORT).show()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun checkForAdminNoteAndShow(userId: String) {
        val adminNoteRef = firebaseDatabaseReference.child("pending_drivers").child(userId).child("note")

        adminNoteRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val adminNote = task.result?.getValue(String::class.java)
                if (adminNote != null && adminNote.isNotEmpty()) {
                    showAdminNoteDialog(adminNote)
                } else {
                    Toast.makeText(this, "Your registration is pending approval.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("DriverRegister", "Failed to fetch admin note: ${task.exception?.message}")
            }
        }
    }



    private fun navigateToDashboard() {
        // Navigate to the dashboard activity
        val intent = Intent(this, DriverDashboard::class.java) // Replace with your actual dashboard activity
        startActivity(intent)
        finish() // Close the registration activity
    }

//    private fun incrementPendingDriversCount() {
//        val pendingDriversRef = firebaseDatabaseReference.child("pending_drivers")
//        pendingDriversRef.runTransaction(object : Transaction.Handler {
//            override fun doTransaction(mutableData: MutableData): Transaction.Result {
//                val currentCount = mutableData.value as? Long ?: 0
//                mutableData.value = currentCount + 1
//                return Transaction.success(mutableData)
//            }
//
//            override fun onComplete(
//                error: DatabaseError?,
//                committed: Boolean,
//                dataSnapshot: DataSnapshot?
//            ) {
//                if (error != null) {
//                    Log.e("DriverRegister", "Failed to increment pending drivers count: ${error.message}")
//                } else if (committed) {
//                    Log.d("DriverRegister", "Pending drivers count incremented successfully.")
//                }
//            }
//        })
//    }


    private fun saveImagesAsBase64(userId: String) {
        val updates = mutableMapOf<String, String?>()

        // Only encode images if they exist
        binding.driverAdd.drawable?.toBitmap()?.let { bitmap ->
            encodeImageToBase64(bitmap)?.let { base64 ->
                updates["profileImage"] = base64
            }
        }
        binding.plateNumberAdd.drawable?.toBitmap()?.let { bitmap ->
            encodeImageToBase64(bitmap)?.let { base64 ->
                updates["plateNumberImage"] = base64
            }
        }
        binding.driversLicenseAdd.drawable?.toBitmap()?.let { bitmap ->
            encodeImageToBase64(bitmap)?.let { base64 ->
                updates["licenceImage"] = base64
            }
        }
        binding.tricycleAdd.drawable?.toBitmap()?.let { bitmap ->
            encodeImageToBase64(bitmap)?.let { base64 ->
                updates["tricycleImage"] = base64
            }
        }

        if (updates.isNotEmpty()) {
            firebaseDatabaseReference.child("pending_drivers").child(userId)
                .updateChildren(updates as Map<String, Any>).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Images saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ImageSave", "Failed to save images: ${task.exception?.message}")
                        Toast.makeText(this, "Failed to save images", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Log.w("ImageSave", "No images to save")
            Toast.makeText(this, "No images provided", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
        return password.matches(Regex(passwordPattern))
    }


    private fun showProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissal by tapping outside
        progressDialog = builder.create()
        progressDialog.show()
    }

    private fun addDriverPhoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    private fun addPlateNumber() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE + 1)
    }

    private fun addTricycle() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE + 2)
    }

    private fun addDriversLicense() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE + 3)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            try {
                selectedImageUri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    when (requestCode) {
                        IMAGE_REQUEST_CODE -> {
                            binding.driverAdd.setImageBitmap(bitmap)
                        }
                        IMAGE_REQUEST_CODE + 1 -> {
                            binding.plateNumberAdd.setImageBitmap(bitmap)
                        }
                        IMAGE_REQUEST_CODE + 2 -> {
                            binding.tricycleAdd.setImageBitmap(bitmap)
                        }
                        IMAGE_REQUEST_CODE + 3 -> {
                            binding.driversLicenseAdd.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageSelection", "Error selecting image: ${e.message}")
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int = 800, maxHeight: Int = 800): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val aspectRatio = width.toFloat() / height.toFloat()

        var newWidth = maxWidth
        var newHeight = (maxWidth / aspectRatio).toInt()

        if (height > width) {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun encodeImageToBase64(bitmap: Bitmap?): String? {
        if (bitmap == null) {
            Log.e("ImageConversion", "Bitmap is null")
            return null
        }
        return try {
            val resizedBitmap = resizeBitmap(bitmap) // Resize before encoding
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ImageConversion", "Error encoding image: ${e.message}")
            null
        }
    }

    private fun saveImageUrlToDatabase(base64String: String, key: String) {
        val userId = firebaseAuth.currentUser ?.uid ?: return
        val userRef = firebaseDatabaseReference.child("drivers_photo").child(userId)

        userRef.child(key).setValue(base64String)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("DATABASE", "Failed to save image: ${task.exception?.message}")
                }
            }
    }
}