package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.R
import com.aiavatar.app.autoCleared
import com.aiavatar.app.databinding.FragmentWalkThroughBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.presentation.utils.FragmentPagerAdapter
import com.aiavatar.app.safeCall
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle

class WalkThroughFragment : Fragment() {

    private var _binding: FragmentWalkThroughBinding by autoCleared()
    val binding: FragmentWalkThroughBinding
        get() = _binding!!

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
        _binding = FragmentWalkThroughBinding.bind(view)

        binding.bindState()
        handleBackPressed()
    }

    private fun FragmentWalkThroughBinding.bindState() {
        // TODO: -done- setup adapter
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
                if (checkLegal()) {
                    try {
                        ApplicationDependencies.getPersistentStore()
                            .setOnboardPresented(true)
                        gotoUploadSteps()
                        /*findNavController().apply {
                            val args = bundleOf(
                                Constant.EXTRA_FROM to "walk_through"
                            )
                            val navOpts = NavOptions.Builder()
                                .setEnterAnim(R.anim.fade_scale_in)
                                .setExitAnim(R.anim.fade_scale_out)
                                .setPopUpTo(R.id.walkthrough_fragment, inclusive = true, saveState = true)
                                .build()
                            navigate(R.id.login_fragment, args, navOpts)
                        }*/
                    } catch (ignore: Exception) {}
                } else {
                    showLegal {
                        ApplicationDependencies.getPersistentStore()
                            .setOnboardPresented(true)
                        gotoUploadSteps()
                    }
                }
            }
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    safeCall {
                        if (binding.walkthroughPager.currentItem > 0) {
                            // consume
                            val curPos = binding.walkthroughPager.currentItem
                            binding.walkthroughPager.currentItem = curPos - 1
                        } else {
                            if (!findNavController().popBackStack()) {
                                activity?.finishAffinity()
                            }
                        }
                    }
                }
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    private fun gotoUploadSteps() = safeCall {
        findNavController().apply {
            navigate(WalkThroughFragmentDirections.actionWalkThroughToUploadStep())
        }
    }

    private fun checkLegal(): Boolean {
        return ApplicationDependencies.getPersistentStore().isLegalAgreed
    }

    private fun showLegal(cont: () -> Unit) {
        val f = childFragmentManager.findFragmentByTag(LegalsBottomSheet.FRAGMENT_TAG)
        if (f == null) {
            LegalsBottomSheet {
                ApplicationDependencies.getPersistentStore().setLegalAgreed()
                cont.invoke()
            }.also {
                it.show(childFragmentManager, LegalsBottomSheet.FRAGMENT_TAG)
            }
        }
    }
}