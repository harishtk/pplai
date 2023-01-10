package com.pepulai.app.feature.home.presentation.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pepulai.app.Constant
import com.pepulai.app.R
import com.pepulai.app.databinding.FragmentCatalogBinding
import com.pepulai.app.feature.home.domain.model.Category
import com.pepulai.app.feature.home.presentation.util.CatalogAdapter
import com.pepulai.app.showToast
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
            }
        }


        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }


        val usersFlow = uiState.mapNotNull { it.userAndCategory }
            .map { it.users }
        viewLifecycleOwner.lifecycleScope.launch {
            usersFlow.collectLatest { users ->
                // Noop
            }
        }

        val catalogAdapterCallback = object : CatalogAdapter.Callback {
            override fun onCardClicked(position: Int, cardPosition: Int) {
                val catalogUiModel = uiState.value.catalogList?.get(position)!!
                catalogUiModel as CatalogUiModel.Catalog
                gotoCatalogDetail(catalogUiModel.category, cardPosition)
            }

            override fun onMoreClicked(position: Int) {
                val catalogUiModel = uiState.value.catalogList?.get(position)!!
                catalogUiModel as CatalogUiModel.Catalog
                gotoCatalogDetail(catalogUiModel.category)
            }
        }

        val catalogAdapter = CatalogAdapter(catalogAdapterCallback)
        bindList(
            adapter = catalogAdapter,
            uiState = uiState
        )

        bindToolbar()
    }

    private fun FragmentCatalogBinding.bindList(
        adapter: CatalogAdapter,
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

    private fun FragmentCatalogBinding.bindToolbar() {
        toolbarIncluded.toolbarNavigationIcon.isVisible = false
        toolbarIncluded.toolbarTitle.text = "Pepul AI"
    }

    private fun gotoCatalogDetail(category: Category, cardClickPosition: Int = -1) {
        findNavController().apply {
            val args = Bundle()
            args.putParcelable(Constant.EXTRA_DATA, category)
            args.putInt("click_position", cardClickPosition)
            navigate(R.id.action_catalog_list_to_catalog_detail, args)
        }
    }
}