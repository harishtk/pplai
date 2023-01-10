package com.pepulai.app.feature.home.presentation.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.pepulai.app.Constant
import com.pepulai.app.R
import com.pepulai.app.databinding.FragmentCatalogDetailBinding
import com.pepulai.app.feature.home.domain.model.Category
import com.pepulai.app.feature.home.presentation.util.CatalogPagerAdapter
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class CatalogDetailFragment : Fragment() {

    private var _binding: FragmentCatalogDetailBinding? = null
    private val binding: FragmentCatalogDetailBinding
        get() = _binding!!

    private val viewModel: CatalogDetailViewModel by viewModels()

    private var jumpToPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val category = arguments?.getParcelable<Category?>(Constant.EXTRA_DATA)
        jumpToPosition = arguments?.getInt("click_position", -1) ?: -1
        if (category != null) {
            viewModel.setCatalog(category)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_catalog_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCatalogDetailBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState
        )
    }

    private fun FragmentCatalogDetailBinding.bindState(
        uiState: StateFlow<CatalogDetailState>
    ) {
        val catalogPresetAdapter = CatalogPagerAdapter(requireContext())

        val pageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(80))
            addTransformer(ViewPager2.PageTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + (r * 0.15f)
            })
        }

        catalogPreviewPager.apply {
            adapter = catalogPresetAdapter
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            offscreenPageLimit = 3
            clipChildren = false
            clipToPadding = false

            setPageTransformer(pageTransformer)
        }

        /*circleIndicator.setViewPager(catalogPreviewPager)
        catalogPresetAdapter.registerAdapterDataObserver(circleIndicator.adapterDataObserver)*/

        val indicatorSizePx = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val normalColor = resources.getColor(R.color.grey_divider, null)
        val checkedColor = resources.getColor(R.color.white, null)
        binding.indicatorView.apply {
            setSliderColor(checkedColor, checkedColor)
            // setCheckedSlideWidth((indicatorSizePx * 2).toFloat())
            // setSliderWidth(indicatorSizePx.toFloat())
            setSliderWidth(indicatorSizePx.toFloat(), (indicatorSizePx * 3).toFloat())
            setSliderHeight(indicatorSizePx.toFloat())
            setSlideMode(IndicatorSlideMode.SCALE)
            setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            notifyDataChanged()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            uiState.mapNotNull { it.category?.preset }
                .collectLatest { catalogList ->
                    catalogPresetAdapter.submitList(catalogList)
                    setUpIndicator(catalogList.size)
                    if (jumpToPosition != -1) {
                        catalogPreviewPager.setCurrentItem(jumpToPosition, false)
                        // setUpCurrentIndicator(jumpToPosition)
                        jumpToPosition = -1
                    } else {
                        // setUpCurrentIndicator(0)
                    }
                }
        }

        catalogPreviewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // setUpCurrentIndicator(position)
                binding.indicatorView.onPageSelected(position)
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                binding.indicatorView.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                binding.indicatorView.onPageScrollStateChanged(state)
            }
        })

        bindToolbar(
            uiState = uiState
        )
    }

    private fun FragmentCatalogDetailBinding.bindToolbar(uiState: StateFlow<CatalogDetailState>) {
        val catalogTitleFlow = uiState.mapNotNull { it.category?.title }
        viewLifecycleOwner.lifecycleScope.launch {
            catalogTitleFlow.collectLatest { catalogTitle ->
                toolbarIncluded.toolbarTitle.text = catalogTitle
            }
        }

        toolbarIncluded.toolbarNavigationIcon.setOnClickListener {
            try { findNavController().navigateUp() }
            catch (ignore: Exception) {}
        }
    }

    private fun setUpIndicator(count: Int) {
        binding.indicatorView.setPageSize(count)
        binding.indicatorView.notifyDataChanged()
        /*val indicators = arrayOfNulls<View>(count)
        *//*val displayMetrics = Resources.getSystem().displayMetrics
        val tabIndicatorWidth = displayMetrics.widthPixels * 0.1
        val tabIndicatorHeight = tabIndicatorWidth * 0.1*//*
        val tabIndicatorWidth = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val tabIndicatorHeight = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(tabIndicatorWidth.toInt(), tabIndicatorHeight.toInt(), 1f)
        if (binding.pagerIndicators.orientation == LinearLayout.HORIZONTAL) {
            layoutParams.setMargins(10, 0, 10, 0)
        } else {
            layoutParams.setMargins(0, 10, 0, 10)
        }

//        View(requireContext()).apply {
//            this.background = ResourcesCompat.getDrawable(resources, R.drawable.grey_curved_bg, null)
//            this.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.white, null))
//            this.layoutParams = layoutParams
//            this.layoutParams.width = tabIndicatorWidth * 2
//        }.also { maskedIndicator ->
//            binding.pagerIndicators.addView(maskedIndicator)
//        }

        for (i in indicators.indices) {
            indicators[i] = View(requireContext())
            indicators[i]?.apply {
                // this.setImageResource(R.drawable.grey_curved_bg)
                this.background = ResourcesCompat.getDrawable(resources, R.drawable.grey_curved_bg, null)
                this.backgroundTintList = (resources.getColorStateList(R.color.selector_indicator, null))
                this.layoutParams = layoutParams
            }
            binding.pagerIndicators.addView(indicators[i])
        }*/
    }

    private fun setUpCurrentIndicator(index: Int) {
        // Log.d(TAG, "setUpCurrentIndicator() called with: index = $index")
        val indicatorSizePx = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        for (i in 0 until binding.pagerIndicators.childCount) {
            // Log.d(TAG, "setUpCurrentIndicator() called with: index = $index i = $i")
            val current = binding.pagerIndicators[i] as? View?
            /*current?.let {
                val lp = it.layoutParams
                if (index == i) {
                    lp.width = indicatorSizePx * 2
                } else {
                    lp.width = indicatorSizePx
                }
                it.layoutParams = lp
                Log.d(TAG, "setUpCurrentIndicator() called with: index = $index i = $i w=${lp.width}")

            }*/
            (binding.pagerIndicators[i] as? View?)
                ?.isSelected = index == i
        }
    }

    companion object {
        val TAG = CatalogDetailFragment::class.java.simpleName
    }
}