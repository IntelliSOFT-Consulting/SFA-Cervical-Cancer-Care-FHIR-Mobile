package com.icl.cervicalcancercare.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.icl.cervicalcancercare.details.child.DiagnosisFragment
import com.icl.cervicalcancercare.details.child.OverviewFragment
import com.icl.cervicalcancercare.details.child.RecommendationFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OverviewFragment()
            1 -> RecommendationFragment()
            2 -> DiagnosisFragment()
            else -> OverviewFragment()
        }
    }
}
