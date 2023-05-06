package com.shrestha.diabeatit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.shrestha.diabeatit.ui.views.profile_fragments.GlucoseFrag
import com.shrestha.diabeatit.VPAdapter
import com.shrestha.diabeatit.ui.views.profile_fragments.WeightFrag
import com.shrestha.diabeatit.databinding.FragmentProfileBinding


@Suppress("DEPRECATION")
class ProfileFragment : Fragment() {
    var vpAdapter: VPAdapter? = null
    private lateinit var gFragment : GlucoseFrag
    private lateinit var wFragment : WeightFrag

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        gFragment = GlucoseFrag()
        wFragment = WeightFrag()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.tabLayout.setupWithViewPager(binding.pager)
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        vpAdapter = VPAdapter(
            fragmentManager, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )

        vpAdapter!!.addFragment(gFragment, "Glucose")
        vpAdapter!!.addFragment(wFragment, "Weight")

        binding.pager.adapter = vpAdapter


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}