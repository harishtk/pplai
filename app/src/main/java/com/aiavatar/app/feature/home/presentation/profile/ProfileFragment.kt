package com.aiavatar.app.feature.home.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentProfileBinding
import com.aiavatar.app.databinding.ItemModelListBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.showToast
import com.bumptech.glide.Glide
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentProfileBinding.bindState(
        uiState: StateFlow<ProfileState>,
        uiAction: (ProfileUiAction) -> Unit,
        uiEvent: SharedFlow<ProfileUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is ProfileUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .map { it.refresh !is LoadState.Loading }
            .distinctUntilChanged()

        val hasErrorsFlow = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        Timber.d(e)
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        uiAction(ProfileUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val callback = object : ModelListAdapter.Callback {
            override fun onItemClick(position: Int, data: ModelListUiModel.Item) {
                // Noop
            }
        }

        val adapter = ModelListAdapter(callback)

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.refresh is LoadState.Loading) {
                    emptyListContainer.isVisible = false
                    progressBar.isVisible = true
                } else {
                    progressBar.isVisible = false
                    emptyListContainer.isVisible = adapter.itemCount <= 0
                }
            }
        }

        bindList(
            adapter = adapter,
            uiState = uiState
        )

        bindClick(
            uiState = uiState
        )

        bindAppbar(
            uiState = uiState
        )
    }

    private fun FragmentProfileBinding.bindList(
        adapter: ModelListAdapter,
        uiState: StateFlow<ProfileState>
    ) {
        modelListView.postDelayed({
            modelListView.adapter = adapter
        }, UI_RENDER_WAIT_TIME)

        val modelListUiModelsFlow = uiState.map { it.modelListUiModels }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            modelListUiModelsFlow.collectLatest { modelList ->
                adapter.submitList(modelList)
            }
        }
    }

    private fun FragmentProfileBinding.bindClick(uiState: StateFlow<ProfileState>) {
        retryButton.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun FragmentProfileBinding.bindAppbar(uiState: StateFlow<ProfileState>) {
        appbarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            Timber.d("Offset: $verticalOffset total: ${appBarLayout.totalScrollRange}")
        }

        toolbarSettings.setOnClickListener { gotoSettings() }
        toolbarNavigationIcon.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (ignore: Exception) {}
        }
        textUsernameExpanded.text = getString(R.string.username_with_prefix,
            ApplicationDependencies.getPersistentStore().username)
    }

    private fun gotoSettings() {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_right)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
            navigate(R.id.action_profile_to_settings, null, navOpts)
        }
    }

    companion object {
        private const val UI_RENDER_WAIT_TIME = 50L
    }
}

class ModelListAdapter(
    private val callback: Callback
) : ListAdapter<ModelListUiModel, ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            else -> {
                throw IllegalStateException("Unknown viewType $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        when (model) {
            is ModelListUiModel.Item -> {
                holder as ItemViewHolder
                holder.bind(data = model, callback)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val model = getItem(position)) {
            is ModelListUiModel.Item -> VIEW_TYPE_ITEM
            else -> {
                throw IllegalStateException("Can't decide a view type for $position")
            }
        }
    }

    class ItemViewHolder private constructor(
        private val binding: ItemModelListBinding
    ) : ViewHolder(binding.root) {

        fun bind(data: ModelListUiModel.Item, callback: Callback) = with(binding) {
            // TODO: bind data
            title.text = data.modelList.modelData?.name
            Glide.with(imageView)
                .load(data.modelList.modelData?.latestImage)
                .placeholder(R.color.transparent_black)
                .error(R.color.white)
                .into(imageView)

            // toggleSelection(selected)

            imageView.setOnClickListener { callback.onItemClick(adapterPosition, data) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_model_list,
                    parent,
                    false
                )
                val binding = ItemModelListBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    interface Callback {
        fun onItemClick(position: Int, data: ModelListUiModel.Item)
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0

        private val DIFF_CALLBACK = object : ItemCallback<ModelListUiModel>() {
            override fun areItemsTheSame(
                oldItem: ModelListUiModel,
                newItem: ModelListUiModel,
            ): Boolean {
                return (oldItem is ModelListUiModel.Item && newItem is ModelListUiModel.Item &&
                        oldItem.modelList.modelData?.id == newItem.modelList.modelData?.id)
            }

            override fun areContentsTheSame(
                oldItem: ModelListUiModel,
                newItem: ModelListUiModel,
            ): Boolean {
                return false
            }
        }
    }

}