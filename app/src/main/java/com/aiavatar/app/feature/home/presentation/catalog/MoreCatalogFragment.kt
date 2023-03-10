package com.aiavatar.app.feature.home.presentation.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.recyclerview.Recyclable
import com.aiavatar.app.core.URLProvider
import com.aiavatar.app.databinding.FragmentMoreCatalogBinding
import com.aiavatar.app.databinding.ItemMoreCatalogBinding
import com.aiavatar.app.feature.home.domain.model.CatalogList
import com.aiavatar.app.feature.home.domain.model.ListAvatar
import com.aiavatar.app.safeCall
import com.bumptech.glide.Glide
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoreCatalogFragment : Fragment() {

    private val viewModel: MoreCatalogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: get the category id and fetch category list
        arguments?.apply {
            getString(ARG_CATALOG_NAME, null)?.let { name ->
                viewModel.setCatalogName(name)
            }
        }
        /*val category = arguments?.getParcelable<Category?>(Constant.EXTRA_DATA)
        if (category != null) {
            viewModel.setCategory(category)
        }*/
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_more_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentMoreCatalogBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept
        )
    }

    private fun FragmentMoreCatalogBinding.bindState(
        uiState: StateFlow<MoreCatalogState>,
        uiAction: (MoreCatalogUiAction) -> Unit
    ) {

        val adapter = MoreCatalogScrollAdapter { clickedPosition ->
            // Noop
        }
        avatarScrollerList.adapter = adapter

        val avatarListFlow = uiState.map { it.avatarList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarListFlow.collectLatest { avatarList ->
                adapter.submitList(avatarList)
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                val emptyList = adapter.itemCount <= 0
                progressBar.isVisible = loadState.refresh is LoadState.Loading &&
                        emptyList
                retryButton.isVisible = loadState.refresh is LoadState.Error &&
                        emptyList
                if (loadState.refresh is LoadState.Error) {
                    HapticUtil.createError(requireContext())
                    retryButton.shakeNow()
                }
            }
        }

        bindClick(
            uiState = uiState
        )

        bindToolbar(
            uiState = uiState
        )
    }

    private fun FragmentMoreCatalogBinding.bindClick(uiState: StateFlow<MoreCatalogState>) {
        btnNext.setOnClickListener {
            gotoUploadSteps()
        }

        retryButton.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun FragmentMoreCatalogBinding.bindToolbar(uiState: StateFlow<MoreCatalogState>) {
        val catalogTitleFlow = uiState.mapNotNull { it.catalogName }
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

    private fun gotoUploadSteps() = safeCall {
        findNavController().apply {
            navigate(MoreCatalogFragmentDirections.actionMoreCatalogToUploadStep1())
        }
    }

    companion object {
        const val ARG_CATEGORY_ID = "com.aiavatar.app.args.CATEGORY_ID"
        const val ARG_CATALOG_NAME = "com.aiavatar.app.args.CATALOG_NAME"
    }
}

class MoreCatalogScrollAdapter(
    private val onCardClick: (position: Int) -> Unit = { }
) : ListAdapter<MoreCatalogUiModel, MoreCatalogScrollAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = getItem(position)
        model as MoreCatalogUiModel.Item
        holder.bind(model.catalogList, model.selected, onCardClick)
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
        private val binding: ItemMoreCatalogBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(preset: CatalogList, selected: Boolean, onCardClick: (position: Int) -> Unit) = with(binding) {
            title.text = preset.imageName
            Glide.with(imageView)
                .load(URLProvider.avatarUrl(preset.imageName))
                .placeholder(R.color.transparent_black)
                .error(R.color.white)
                .into(imageView)

            // toggleSelection(selected)

            imageView.setOnClickListener { onCardClick(adapterPosition) }
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            imageView.alpha = if (selected) {
                1.0F
            } else {
                0.5F
            }
        }

        override fun onViewRecycled() {
            binding.imageView.let { imageView ->
                Glide.with(imageView).clear(null)
                imageView.setImageDrawable(null)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_more_catalog,
                    parent,
                    false
                )
                val binding = ItemMoreCatalogBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    companion object {
        private const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MoreCatalogUiModel>() {
            override fun areItemsTheSame(
                oldItem: MoreCatalogUiModel,
                newItem: MoreCatalogUiModel,
            ): Boolean {
                return (oldItem is MoreCatalogUiModel.Item && newItem is MoreCatalogUiModel.Item &&
                        oldItem.catalogList.id == newItem.catalogList.id)
            }

            override fun areContentsTheSame(
                oldItem: MoreCatalogUiModel,
                newItem: MoreCatalogUiModel,
            ): Boolean {
                return (oldItem is MoreCatalogUiModel.Item && newItem is MoreCatalogUiModel.Item &&
                        oldItem.catalogList == newItem.catalogList && oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: MoreCatalogUiModel,
                newItem: MoreCatalogUiModel
            ): Any? {
                /*val updatePayload = bundleOf()
                when {
                    oldItem is MoreCatalogUiModel.Item && newItem is MoreCatalogUiModel.Item -> {
                        if (oldItem.selected != newItem.selected) {
                            updatePayload.putBoolean(SELECTION_TOGGLE_PAYLOAD, newItem.selected)
                        }
                    }
                }
                return updatePayload*/
                return null
            }

        }
    }
}