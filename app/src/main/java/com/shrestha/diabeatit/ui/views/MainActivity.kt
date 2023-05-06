package com.shrestha.diabeatit.ui.views

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.shrestha.diabeatit.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.max = 1000
        val currentProgress = 1000
        ObjectAnimator.ofInt(binding.progressBar, "progress", currentProgress)
            .setDuration(3000)
            .start()

//        binding.progressBar.setBackgroundColor(Color.parseColor("#53C4F8"))

        Handler().postDelayed({
            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
            finish()
        }, 3200)

    }
}