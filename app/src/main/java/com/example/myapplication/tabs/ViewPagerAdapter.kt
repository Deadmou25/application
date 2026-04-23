package com.example.myapplication.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Адаптер ViewPager2 для двух вкладок главного экрана.
 *
 * Вкладки:
 * - 0: [ControlFragment] — «Управление» (выбор даты/времени и отправка)
 * - 1: [HistoryFragment] — «История» (список отправленных записей)
 *
 * [FragmentStateAdapter] управляет жизненным циклом фрагментов:
 * фрагменты пересоздаются при свайпе для экономии памяти.
 *
 * @param fragmentActivity Хост-Activity (передаётся в родительский конструктор)
 */
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

    /** Создаёт фрагмент по позиции через рефлексию (вызов конструктора без аргументов) */
    override fun createFragment(position: Int): Fragment {
        return fragments[position].getConstructor().newInstance()
    }

    /**
     * Возвращает заголовок вкладки для [TabLayoutMediator].
     * @param position Индекс вкладки (0 или 1)
     */
    fun getPageTitle(position: Int): CharSequence = titles[position]
}
