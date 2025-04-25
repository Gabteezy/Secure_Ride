package com.aces.capstone.secureride

import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityAboutUsBinding

class AboutUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            finish() // Go back when navigation icon is clicked
        }

        // Bold titles for each TextView
        boldTitle(binding.tvIntroduction, "Welcome to SecureRide!")
        boldTitle(binding.tvCreators, "Our Team")
        boldTitle(binding.tvTermsSummary, "Our Commitment to You")
        boldTitle(binding.tvContact, "Contact Us")
    }

    private fun boldTitle(textView: TextView, title: String) {
        val fullText = textView.text.toString()
        val spannable = SpannableString(fullText)
        val titleLength = title.length
        if (fullText.startsWith(title)) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                titleLength,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = spannable
    }
}