package com.shrestha.diabeatit.ui.views

import android.app.Dialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.shrestha.diabeatit.R
import com.shrestha.diabeatit.databinding.ActivityHomeBinding
import com.shrestha.diabeatit.ui.map.MapFragment
import com.shrestha.diabeatit.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var fragment1: MapFragment
    private lateinit var fragment2: ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView


        val navController = findNavController(R.id.nav_host_fragment_activity_home)
        navView.setupWithNavController(navController)
        fragment1 = MapFragment()
        fragment2 = ProfileFragment()




        setFragment(fragment1)

        navView.setOnNavigationItemSelectedListener { menuItem ->
            Log.d(TAG, "onItemSelected: ")
            when (menuItem.itemId) {
                R.id.navigation_menu -> {

                    showDialogue()

                    true
                }


                R.id.navigation_map -> {
                    setFragment(fragment1)
                    true
                }

                R.id.navigation_profile -> {
                    setFragment(fragment2)
                    true
                }
                else -> false
            }
        }

    }

    private fun showDialogue() {
        val dialog = Dialog(this@HomeActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.menu_alert_dialogue)
        dialog.findViewById<ImageView>(R.id.imgCancel).setOnClickListener {
            dialog.cancel()
        }

        dialog.show()
    }

    private fun setFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment_activity_home, fragment)
        transaction.commit()
    }
}