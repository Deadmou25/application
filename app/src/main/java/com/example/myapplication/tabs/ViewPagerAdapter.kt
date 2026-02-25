package com.example.myapplication.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        ControlFragment::class.java,
        HistoryFragment::class.java
    )

    private val titles = listOf(
        "Управление",
        "История"
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position].getConstructor().newInstance()
    }

    fun getPageTitle(position: Int): CharSequence = titles[position]
}