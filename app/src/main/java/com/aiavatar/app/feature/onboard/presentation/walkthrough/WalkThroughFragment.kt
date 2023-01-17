package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentWalkThroughBinding
import com.aiavatar.app.feature.onboard.presentation.utils.FragmentPagerAdapter
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle

class WalkThroughFragment : Fragment() {

    private val SMALL_DESCRIPTIONS = listOf<Int>(
        R.string.walkthrough_des_1,
        R.string.walkthrough_des_2,
        R.string.walkthrough_des_3
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_walk_through, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalkThroughBinding.bind(view)

        binding.bindState()
    }

    private fun FragmentWalkThroughBinding.bindState() {
        // TODO: setup adapter
        val adapter = FragmentPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        adapter.addFragment(WalkThroughContent1())
        adapter.addFragment(WalkThroughContent2())
        adapter.addFragment(WalkThroughContent3())

        walkthroughPager.apply {
            this.adapter = adapter
            offscreenPageLimit = 3
        }

        val indicatorSizePx = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val normalColor = resources.getColor(R.color.tab_indicator_unselected, null)
        val checkedColor = resources.getColor(R.color.tab_indicator_selected, null)
        indicatorView.apply {
            setSliderColor(normalColor, checkedColor)
            // setCheckedSlideWidth((indicatorSizePx * 2).toFloat())
            // setSliderWidth(indicatorSizePx.toFloat())
            setSliderWidth(indicatorSizePx.toFloat(), (indicatorSizePx * 3).toFloat())
            setSliderHeight(indicatorSizePx.toFloat())
            setSlideMode(IndicatorSlideMode.SCALE)
            setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            setPageSize(adapter.itemCount)
            notifyDataChanged()
        }

        walkthroughPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // setUpCurrentIndicator(position)
                indicatorView.onPageSelected(position)
                smallDescription1.text = getString(SMALL_DESCRIPTIONS[position])
                if (position == adapter.itemCount - 1) {
                    btnNext.text = getString(R.string.label_try_now)
                    btnNext.setIconResource(R.drawable.arrow_right)
                } else {
                    btnNext.text = getString(R.string.label_continue)
                    btnNext.icon = null
                }
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                indicatorView.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                indicatorView.onPageScrollStateChanged(state)
            }
        })

        btnNext.setOnClickListener {
            if (walkthroughPager.currentItem < adapter.itemCount - 1) {
                walkthroughPager.setCurrentItem(++walkthroughPager.currentItem, true)
            } else {
                try {
                    findNavController().navigate(R.id.action_walkthrough_fragment_to_login_fragment)
                } catch (ignore: Exception) {}
            }
        }
    }
}