package com.aiavatar.app.commons.presentation.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentWebViewPresenterBinding
import com.aiavatar.app.safeCall
import timber.log.Timber

class WebViewPresenterFragment : Fragment() {

    private lateinit var url: String

    private var webViewWeakRef: WebView? = null
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            url = arguments?.getString(EXTRA_URL, null)!!
        } catch (e: NullPointerException) {
            throw IllegalStateException("No url", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_web_view_presenter, container, false)
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val contextThemeWrapper: Context = ContextThemeWrapper(requireContext(), R.style.Theme_WebView)
        return inflater.cloneInContext(contextThemeWrapper)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWebViewPresenterBinding.bind(view)

        binding.setupWebView()
        setupOnBackPressed()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun FragmentWebViewPresenterBinding.setupWebView() {
        val chromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                if (newProgress < 100 && isFirstLoad) {
                    toolbarIncluded.toolbarTitle.text = getString(R.string.label_loading)
                    progressBar.isVisible = true
                    isFirstLoad = false
                } else {
                    progressBar.isVisible = false
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                Timber.tag(TAG).d("onReceivedTitle() title = $title")
                toolbarIncluded.toolbarTitle.text = title
            }
        }

        webView.apply {
            webChromeClient = chromeClient
            settings.apply {
                setSupportZoom(false)
                javaScriptEnabled = true
            }
            setBackgroundColor(Color.TRANSPARENT)
        }.also {
            it.loadUrl(url)
            webViewWeakRef = it
        }

        bindClick()
    }

    private fun FragmentWebViewPresenterBinding.bindClick() {
        toolbarIncluded.apply {
            toolbarNavigationIcon.setOnClickListener {
                safeCall { findNavController().navigateUp() }
            }
        }
    }

    private fun setupOnBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webViewWeakRef?.canGoBack() == true) {
                    webViewWeakRef!!.goBack()
                } else {
                    findNavController().navigateUp()
                }
            }
        })
    }

    companion object {
        private const val TAG = "WebView.Msg"
        const val EXTRA_URL     = "com.aiavatar.app.extras.URL"

    }
}