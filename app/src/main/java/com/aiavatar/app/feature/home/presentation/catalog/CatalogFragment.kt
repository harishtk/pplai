package com.aiavatar.app.feature.home.presentation.catalog

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
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.AnimationUtil.touchInteractFeedback
import com.aiavatar.app.databinding.FragmentCatalogBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.util.AvatarsAdapter
import com.aiavatar.app.showToast
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CatalogFragment : Fragment() {

    private val viewModel: CatalogViewModel by viewModels()

    private var _binding: FragmentCatalogBinding? = null
    private val binding: FragmentCatalogBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCatalogBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentCatalogBinding.bindState(
        uiState: StateFlow<CatalogState>,
        uiAction: (CatalogUiAction) -> Unit,
        uiEvent: SharedFlow<CatalogUiEvent>
    ) {
        // TODO: bind state
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is CatalogUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                }
            }
        }

        val avatarsAdapterCallback = object : AvatarsAdapter.Callback {
            override fun onItemClick(position: Int) {
                // Noop
            }

        }

        val avatarsAdapter = AvatarsAdapter(avatarsAdapterCallback)

        val notLoadingFlow = uiState.map { it.loadState }
            .map { it.refresh !is LoadState.Loading }
        val hasErrorsFlow = uiState.map { it.exception != null }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    retryButton.isVisible = avatarsAdapter.itemCount <= 0
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        uiAction(CatalogUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.refresh }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadStateFlow ->
                progressBar.isVisible = loadStateFlow.refresh is LoadState.Loading
                swipeRefreshLayout.isRefreshing = loadStateFlow.refresh is LoadState.Loading
                if (loadStateFlow.refresh is LoadState.Loading) {
                    retryButton.isVisible = false
                }
            }
        }

        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        bindList(
            adapter = avatarsAdapter,
            uiState = uiState
        )

        bindClick()

        bindToolbar()
    }

    private fun FragmentCatalogBinding.bindList(
        adapter: AvatarsAdapter,
        uiState: StateFlow<CatalogState>
    ) {
        catalogList.adapter = adapter

        val catalogListFlow = uiState.map { it.catalogList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            catalogListFlow.collectLatest { catalogList ->
                adapter.submitList(catalogList)
            }
        }
    }

    private fun FragmentCatalogBinding.bindClick() {
        btnCreateMasterPiece.setOnClickListener {
            try {
                findNavController().apply {
                    navigate(R.id.action_catalog_list_to_upload_step_1)
                }
            } catch (ignore: Exception) {}
        }

        toolbarTitle.setOnClickListener {
            toolbarTitle.touchInteractFeedback()
            try {
                findNavController().apply {
                    navigate(R.id.action_catalog_list_to_subscription_plans)
                }
            } catch (ignore: Exception) {}
        }

        retryButton.setOnClickListener { viewModel.refresh() }
    }

    private fun FragmentCatalogBinding.bindToolbar() {
        toolbarNavigationIcon.isVisible = false
        toolbarTitle.text = "Avatar"

        ApplicationDependencies.getPersistentStore().apply {
            if (isLogged) {
                val initialLetter = ApplicationDependencies.getPersistentStore().username[0].toString().uppercase()
                profileName.setText(initialLetter)
            } else {
                profileName.setText(null)
                profileImage.setImageResource(R.drawable.ic_account_outline)
            }
        }

        profileContainer.setOnClickListener {
            if (ApplicationDependencies.getPersistentStore().isLogged) {
                gotoProfile()
            } else {
                gotoLogin()
            }
        }
    }

    private fun gotoLogin() {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.fade_scale_in)
                .setExitAnim(R.anim.fade_scale_out)
                .build()
            navigate(R.id.login_fragment, null, navOpts)
        }
    }

    private fun gotoProfile() {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_right)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
            navigate(R.id.action_catalog_list_to_profile, null, navOpts)
        }
    }

    /*private fun gotoCatalogDetail(category: Category, cardClickPosition: Int = -1) {
        findNavController().apply {
            val args = Bundle()
            args.putParcelable(Constant.EXTRA_DATA, category)
            args.putInt("click_position", cardClickPosition)
            navigate(R.id.action_catalog_list_to_catalog_detail, args)
        }
    }*/
}