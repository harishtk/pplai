package com.aiavatar.app.feature.home.presentation.maintenance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentMaintenanceBinding
import com.aiavatar.app.safeCall
import timber.log.Timber


/**
 * @author Hariskumar Kubendran
 * @date 03/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
class MaintenanceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_maintenance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentMaintenanceBinding.bind(view)

        binding.bindState()
    }

    private fun FragmentMaintenanceBinding.bindState() {

        btnClose.setOnClickListener {
            safeCall {
                findNavController().apply {
                    if (!navigateUp()) {
                        if (!popBackStack()) {
                            activity?.finishAffinity()
                        }
                    }
                }
            }
        }
    }
}