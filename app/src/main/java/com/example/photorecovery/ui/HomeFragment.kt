package com.example.photorecovery.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.photorecovery.MainActivity
import com.example.photorecovery.R
import com.example.photorecovery.databinding.FragmentHomeBinding
import com.example.photorecovery.util.PermissionUtils
import com.example.photorecovery.util.RootUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** 主页：选择扫描模式，处理权限。 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MediaViewModel by activityViewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            updatePermissionUi(result)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePermissionUi(emptyMap())

        binding.btnGrant.setOnClickListener { requestPermissions() }
        binding.btnQuick.setOnClickListener {
            if (permissionGranted()) startQuickScan() else requestPermissions()
        }
        binding.btnDeep.setOnClickListener {
            if (!RootUtils.hasRoot()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.need_root_title)
                    .setMessage(R.string.need_root_msg)
                    .setPositiveButton(R.string.confirm, null)
                    .show()
            } else {
                startDeepScan()
            }
        }
    }

    private fun startQuickScan() {
        viewModel.startQuickScan()
        (activity as MainActivity).navigateTo(ResultsFragment())
    }

    private fun startDeepScan() {
        viewModel.startDeepScan()
        (activity as MainActivity).navigateTo(ResultsFragment())
    }

    private fun requestPermissions() {
        permissionLauncher.launch(PermissionUtils.requiredPermissions())
    }

    private fun permissionGranted(): Boolean {
        val grants = mutableMapOf<String, Boolean>()
        for (p in PermissionUtils.requiredPermissions()) {
            grants[p] = ContextCompat.checkSelfPermission(requireContext(), p) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return PermissionUtils.hasPermissions(grants)
    }

    private fun updatePermissionUi(result: Map<String, Boolean>) {
        val granted = if (result.isEmpty()) permissionGranted()
        else PermissionUtils.hasPermissions(result)

        binding.tvPermission.setText(if (granted) R.string.permission_ok else R.string.permission_need)
        binding.btnGrant.visibility = if (granted) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
