package com.aiavatar.app.feature.home.presentation.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.recyclerview.Recyclable
import com.aiavatar.app.core.URLProvider
import com.aiavatar.app.databinding.FragmentModelDetailBinding
import com.aiavatar.app.databinding.ItemScrollerListBinding
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.domain.model.ListAvatar
import com.aiavatar.app.feature.home.presentation.util.CatalogPagerAdapter
import com.bumptech.glide.Glide
import com.pepulnow.app.data.LoadState
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class ModelDetailFragment : Fragment() {

    private var _binding: FragmentModelDetailBinding? = null
    private val binding: FragmentModelDetailBinding
        get() = _binding!!

    private val viewModel: ModelDetailViewModel by viewModels()

    private var jumpToPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val category = arguments?.getParcelable<Category?>(Constant.EXTRA_DATA)
        jumpToPosition = arguments?.getInt("click_position", -1) ?: -1
        if (category != null) {
            viewModel.setCategory(category)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_model_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentModelDetailBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState
        )
    }

    private fun FragmentModelDetailBinding.bindState(
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

        // TODO: get catalog detail list

        catalogPreviewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // setUpCurrentIndicator(position)
                binding.indicatorView.onPageSelected(position)
                viewModel.toggleSelection(position)
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

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                progressBar.isVisible = loadState.refresh is LoadState.Loading &&
                        catalogPresetAdapter.itemCount <= 0
            }
        }

        val scrollerAdapter = AvatarScrollAdapter { clickedPosition ->
            catalogPreviewPager.setCurrentItem(clickedPosition, true)
        }

        val avatarListFlow = uiState.map { it.avatarList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarListFlow.collectLatest { avatarList ->
                catalogPresetAdapter.submitList(avatarList)
                scrollerAdapter.submitList(avatarList)
                setUpIndicator(avatarList.size)
            }
        }

        avatarScrollerList.adapter = scrollerAdapter

        bindToolbar(
            uiState = uiState
        )
    }

    private fun FragmentModelDetailBinding.bindToolbar(uiState: StateFlow<CatalogDetailState>) {
        val catalogTitleFlow = uiState.mapNotNull { it.category?.categoryName }
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
        val TAG = ModelDetailFragment::class.java.simpleName
    }
}

class AvatarScrollAdapter(
    private val onCardClick: (position: Int) -> Unit = { }
) : ListAdapter<SelectableAvatarUiModel, AvatarScrollAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = getItem(position)
        model as SelectableAvatarUiModel.Item
        holder.bind(model.listAvatar, model.selected, onCardClick)
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            if (isValidPayload(payloads)) {
                val bundle = (payloads.firstOrNull() as? Bundle) ?: kotlin.run {
                    super.onBindViewHolder(holder, position, payloads); return
                }
                if (bundle.containsKey(SELECTION_TOGGLE_PAYLOAD)) {
                    (holder as? ItemViewHolder)?.toggleSelection(bundle.getBoolean(
                        SELECTION_TOGGLE_PAYLOAD, false))
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun isValidPayload(payloads: MutableList<Any>?): Boolean {
        return (payloads?.firstOrNull() as? Bundle)?.keySet()?.any {
            it == SELECTION_TOGGLE_PAYLOAD
        } ?: false
    }

    class ItemViewHolder private constructor(
        private val binding: ItemScrollerListBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(preset: ListAvatar, selected: Boolean, onCardClick: (position: Int) -> Unit) = with(binding) {
            title.text = preset.imageName
            Glide.with(previewImage)
                .load(URLProvider.avatarUrl(preset.imageName))
                .placeholder(R.color.transparent_black)
                .error(R.color.white)
                .into(previewImage)

            toggleSelection(selected)

            previewImage.setOnClickListener { onCardClick(adapterPosition) }
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            previewImage.alpha = if (selected) {
                1.0F
            } else {
                0.5F
            }
        }

        override fun onViewRecycled() {
            binding.previewImage.let { imageView ->
                Glide.with(imageView).clear(null)
                imageView.setImageDrawable(null)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_scroller_list,
                    parent,
                    false
                )
                val binding = ItemScrollerListBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    companion object {
        private const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectableAvatarUiModel>() {
            override fun areItemsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.listAvatar.id == newItem.listAvatar.id)
            }

            override fun areContentsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.listAvatar == newItem.listAvatar && oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel
            ): Any {
                val updatePayload = bundleOf()
                when {
                    oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item -> {
                        if (oldItem.selected != newItem.selected) {
                            updatePayload.putBoolean(SELECTION_TOGGLE_PAYLOAD, newItem.selected)
                        }
                    }
                }
                return updatePayload
            }

        }
    }
}