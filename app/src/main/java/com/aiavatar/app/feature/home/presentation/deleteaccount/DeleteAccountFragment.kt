package com.aiavatar.app.feature.home.presentation.deleteaccount

import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.*
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.util.*
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.core.util.InvalidEmailException
import com.aiavatar.app.databinding.FragmentDeleteAccountBinding
import com.aiavatar.app.databinding.ItemDeleteQuestionnaireOptionsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.Serializable
import javax.inject.Inject

/**
 * TODO: have a dedicated otp verification screen.
 * TODO: clean up to use ui state
 */
@AndroidEntryPoint
class DeleteAccountFragment : Fragment() {

    @Inject lateinit var analyticsLogger: AnalyticsLogger

    private val viewModel: DeleteAccountViewModel by viewModels()

    private var binding: FragmentDeleteAccountBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_delete_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDeleteAccountBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentDeleteAccountBinding.bindState(
        uiState: StateFlow<DeleteAccountUiState>,
        uiAction: (DeleteAccountUiAction) -> Unit,
        uiEvent: SharedFlow<DeleteAccountUiEvent>
    ) {
        toolbarIncluded.toolbarNavigationIcon.setOnClickListener { findNavController().navigateUp() }
        toolbarIncluded.toolbarTitle.text = resources.getString(R.string.delete_my_account)

        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is DeleteAccountUiEvent.ShowSnack -> {
                        root.showSnack(event.message.asString(requireContext()))
                    }
                    is DeleteAccountUiEvent.NextScreen -> {
                        /*val mobileNumber = uiState.value.typedPhone
                        val countryCodeModel = uiState.value.countryCodeModel ?: CountryCodeModel.India
                        val title = uiState.value.title.nullAsEmpty()
                        val description = uiState.value.description.nullAsEmpty()
                       moveToOTPScreen(countryCodeModel, mobileNumber, title, description)*/
                    }
                }
            }
        }

        val arr1 = resources.getStringArray(R.array.delete_account_pt_entries).toMutableList()
            .map { "\u2022 $it" }
        list1.adapter = SimpleListAdapter().apply {
            submitList(arr1)
        }

        resources.getStringArray(R.array.delete_account_questionnaire_opts).toMutableList()
            .map { QuestionnaireOpt(title = it) }.let { questionnaireOpts ->
                viewModel.setQuestionnaireOpts(questionnaireOpts)
            }
        list2.setHasFixedSize(true)
        val checkableAdapter = CheckableAdapter { data, checked ->
            Timber.d("onCheckedChanged: checked = $checked data = $data")
            viewModel.onQuestionnaireToggle(data, checked)
            val title = (binding.list2.adapter as CheckableAdapter).currentList.filter { it.checked }
                .joinToString { it.title }
            uiAction(DeleteAccountUiAction.TypingTitle(title))
        }
        list2.adapter = checkableAdapter

        val questionnaireOptsFlow = uiState.map { it.questionnaireOpts }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            questionnaireOptsFlow.collectLatest { opts ->
                checkableAdapter.submitList(opts)
            }
        }

        val maxLength = resources.getInteger(R.integer.max_delete_account_comment_length)
        edExperience.addTextChangedListener {
            val typedString = it.toString()
            edCharacterCounter.text = "${typedString.length} / ${maxLength}"
        }

        edExperience.setOnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE &&
                keyEvent.action == KeyEvent.ACTION_UP
            ) {
                /*when {
                    deleteButton.text == getString(R.string.request_otp) ->
                        requestOtp(uiActions, edExperience.text.toString().trim())
                }*/
            }
            return@setOnEditorActionListener false
        }

        deleteButton.text = resources.getString(R.string.delete_my_account)
        deleteButton.setOnClickListener {
            context?.showToast("Please verify your email to proceed")
            // edPhone.requestFocus()
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.action is LoadState.Loading) {
                    deleteButton.setSpinning()
                } else {
                    deleteButton.cancelSpinning()
                    if (loadState.action is LoadState.Error) {
                        deleteButton.shakeNow()
                    }
                }

                content.isVisible = loadState.refresh !is LoadState.Loading &&
                        loadState.refresh !is LoadState.Error
                progressBar.isVisible = loadState.refresh is LoadState.Loading
            }
        }

        val notLoading = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh != new.refresh || old.action != new.action
            }
            .map { it.refresh !is LoadState.Loading && it.action !is LoadState.Loading }

        val hasErrors = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                notLoading,
                hasErrors,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorMessage
                    if (e != null) {
                        Timber.d(e)
                        uiErr?.let { root.showSnack(uiErr.asString(requireContext())) }
                        when (e) {
                            is ResolvableException -> {
                                // TODO: do what?
                                when (e.cause) {
                                    is InvalidEmailException -> {
                                        val editTextRect = Rect()
                                        edEmail.requestRectangleOnScreen(editTextRect)
                                        content.requestChildRectangleOnScreen(edEmail, editTextRect, false)
                                    }
                                }
                            }
                        }
                        uiAction(DeleteAccountUiAction.ErrorShown(e))
                    }
                }
            }
        }

        bindInput(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentDeleteAccountBinding.bindClick(
        uiState: StateFlow<DeleteAccountUiState>,
        uiAction: (DeleteAccountUiAction) -> Unit
    ) {

        deleteButton.setOnClickListener {
            uiAction(DeleteAccountUiAction.Validate(false))
            // analyticsLogger.logEvent(Analytics.Event.DELETE_ACCOUNT_GET_OTP_CLICKED, null)
            edEmail.hideKeyboard()
            /*if (toggleSwitch.isChecked) {
                validationBeforeApi()
            } else {
                root.showSnack(getString(R.string.help_description_read_and_accept_terms))
                getOtpBtn.shakeNow()
            }*/
        }
    }

    private fun FragmentDeleteAccountBinding.bindInput(
        uiState: StateFlow<DeleteAccountUiState>,
        uiAction: (DeleteAccountUiAction) -> Unit
    ) {
        val typedEmail = uiState.map { it.typedEmail }
            .distinctUntilChanged()

        val enableOtpButton = typedEmail
            .map { it.isNotBlank() }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            enableOtpButton.collectLatest {
                deleteButton.setEnabledWithAlpha(it)
            }
        }

        edEmail.addTextChangedListener(
            afterTextChanged = { typed ->
                uiAction(DeleteAccountUiAction.TypingEmail(typed.toString().trim()))
            }
        )

        edEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                uiAction(DeleteAccountUiAction.Validate(false))
                true
            } else {
                false
            }
        }

        edEmail.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                uiAction(DeleteAccountUiAction.Validate(false))
                true
            } else {
                false
            }
        }

        edExperience.addTextChangedListener(
            afterTextChanged = { typed ->
                uiAction(DeleteAccountUiAction.TypingDescription(typed.toString().trim()))
            }
        )
    }

   /* private fun requestOtp(
        requestOtpAction: (DeleteAccountUiAction.RequestOtp) -> Unit,
        typedNumber: String,
        recaptchaToken: String? = null,
    ) {
        // requestOtpAction(UiAction.RequestOtp)
        // TODO: Add title and description accordingly
        // TODO: Verify OTP against loginUserId
        val title = (binding.list2.adapter as CheckableAdapter).currentList.filter { it.checked }
            .joinToString { it.title }
        val description = binding.edExperience.text.toString().trim()
        countryCodeModel?.let {
            viewModel.requestOtp(
                loginUserId = userId,
                deviceToken = deviceToken,
                mobileNumber = typedNumber,
                countryCode = it.dialcode.removePrefix("+"),
                title = title,
                description = description,
                recaptchaToken = recaptchaToken,
                loadState = { loadStates -> },
                successContinuation = { uiText, response ->
                    if (response.data.get("isRecaptcha").asBoolean) {
                        triggerSafetyNet { newRecaptchaToken ->
                            requestOtp(requestOtpAction, typedNumber, newRecaptchaToken)
                        }
                    } else if (response.data.get("isOTPSent").asBoolean) {
                        moveToOTPScreen(it.dialcode.removePrefix("+"), typedNumber, false)
                    }
                },
                failureContinuation = { t, uiText ->
                    Timber.e(t)
                    binding.root.showSnack(
                        message = uiText.asString(requireContext()),
                        withBottomNavigation = true
                    )
                }
            )
        }
    }*/

    class CheckableAdapter(
        private val onCheckedChanged: (data: QuestionnaireOpt, checked: Boolean) -> Unit
    ) : ListAdapter<QuestionnaireOpt, CheckableAdapter.ViewHolder>(DIFF_CALLBACK) {

        class ViewHolder(
            private val binding: ItemDeleteQuestionnaireOptionsBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                data: QuestionnaireOpt,
                onCheckedChanged: (data: QuestionnaireOpt, checked: Boolean) -> Unit
            ) = with(binding) {
                title.text = data.title
                title.isSelected = data.checked
                ivCheckbox.isSelected = data.checked

                root.setOnClickListener {
                    onCheckedChanged(data, data.checked.not())
                }
                /*checkbox.setOnCheckedChangeListener { _, checked ->
                    onCheckedChanged(data, checked)
                    title.isSelected = data.checked
                }*/
            }

            companion object {
                fun from(parent: ViewGroup): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_delete_questionnaire_options,
                        parent,
                        false
                    )
                    val binding = ItemDeleteQuestionnaireOptionsBinding.bind(itemView)
                    return ViewHolder(binding)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder.from(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), onCheckedChanged)
        }

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<QuestionnaireOpt>() {
                override fun areItemsTheSame(
                    oldItem: QuestionnaireOpt,
                    newItem: QuestionnaireOpt
                ): Boolean {
                    return oldItem.title == newItem.title && oldItem.checked == newItem.checked
                }

                override fun areContentsTheSame(
                    oldItem: QuestionnaireOpt,
                    newItem: QuestionnaireOpt
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    class SimpleListAdapter : ListAdapter<String, SimpleListAdapter.ViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder.from(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            fun bind(title: String) = with(itemView as TextView) {
                TextViewCompat.setTextAppearance(this, R.style.TextStyle_VerySmall)
                setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.text_secondary_alt,
                        null
                    )
                )
                text = title
            }

            companion object {
                fun from(parent: ViewGroup): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        android.R.layout.simple_list_item_1, parent, false
                    )
                    return ViewHolder(itemView)
                }
            }
        }

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    data class QuestionnaireOpt(
        val title: String,
        var checked: Boolean = false
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as QuestionnaireOpt

            if (title != other.title) return false
            if (checked != other.checked) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + checked.hashCode()
            return result
        }
    }

    companion object {
        const val TAG = "DeleteAccountFragment.Msg"
    }
}