package com.example.photorecovery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photorecovery.R
import com.example.photorecovery.data.MediaItem
import com.example.photorecovery.data.MediaType
import com.example.photorecovery.databinding.FragmentResultsBinding
import com.google.android.material.snackbar.Snackbar

/** 结果页：展示扫描到的项目、筛选、全选与恢复。 */
class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MediaViewModel by activityViewModels()

    private lateinit var adapter: MediaAdapter
    private var currentFilter: MediaType? = null
    private var updatingCheck = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MediaAdapter(
            resolver = requireContext().contentResolver,
            onToggle = { viewModel.toggleSelect(it.key) },
            isSelected = { viewModel.selectedKeys.value.orEmpty().contains(it.key) }
        )
        binding.recyclerMedia.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerMedia.adapter = adapter

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipVideo -> MediaType.VIDEO
                R.id.chipPhoto -> MediaType.PHOTO
                else -> null
            }
            refresh()
        }

        binding.chkSelectAll.setOnCheckedChangeListener { _, checked ->
            if (updatingCheck) return@setOnCheckedChangeListener
            if (checked) {
                viewModel.selectAll(filteredItems().map { it.key }.toSet())
            } else {
                viewModel.selectAll(emptySet())
            }
        }

        binding.btnRestore.setOnClickListener {
            viewModel.restoreSelected { ok, err ->
                val msg = if (err == null) getString(R.string.restore_finished, ok)
                else getString(R.string.restore_failed, err)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.items.observe(viewLifecycleOwner) { refresh() }
        viewModel.selectedKeys.observe(viewLifecycleOwner) { refresh() }
        viewModel.scanning.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) {
            if (it == "ROOT_NOT_AVAILABLE") {
                Snackbar.make(binding.root, R.string.root_unavailable, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun filteredItems(): List<MediaItem> {
        val all = viewModel.items.value.orEmpty()
        return if (currentFilter == null) all else all.filter { it.type == currentFilter }
    }

    private fun refresh() {
        val shown = filteredItems()
        adapter.items = shown
        binding.tvResultTitle.text = getString(R.string.found_items, shown.size)
        binding.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        binding.btnRestore.visibility = if (shown.isEmpty()) View.GONE else View.VISIBLE

        // 同步全选勾选状态（避免触发监听器造成循环）
        val keys = shown.map { it.key }.toSet()
        val selected = viewModel.selectedKeys.value.orEmpty()
        updatingCheck = true
        binding.chkSelectAll.isChecked = keys.isNotEmpty() && selected.containsAll(keys)
        updatingCheck = false

        binding.btnRestore.isEnabled = viewModel.selectedCount > 0
        binding.btnRestore.text = getString(R.string.restore_selected, viewModel.selectedCount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
