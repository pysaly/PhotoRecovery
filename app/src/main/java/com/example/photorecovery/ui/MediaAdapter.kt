package com.example.photorecovery.ui

import android.content.ContentResolver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.photorecovery.data.MediaItem
import com.example.photorecovery.data.MediaType
import com.example.photorecovery.databinding.ItemMediaBinding
import com.example.photorecovery.util.ThumbnailLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 媒体缩略图网格适配器。 */
class MediaAdapter(
    private val resolver: ContentResolver,
    private val onToggle: (MediaItem) -> Unit,
    private val isSelected: (MediaItem) -> Boolean
) : RecyclerView.Adapter<MediaAdapter.VH>() {

    var items: List<MediaItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        scope.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    inner class VH(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        private var thumbJob: Job? = null

        fun bind(item: MediaItem) {
            thumbJob?.cancel()
            binding.root.setOnClickListener { onToggle(item) }

            val selected = isSelected(item)
            binding.imgOverlay.visibility = if (selected) View.VISIBLE else View.GONE
            binding.tvType.text = if (item.type == MediaType.VIDEO) "视频" else "照片"
            binding.tvSize.text = formatSize(item.size)
            binding.imgThumb.setImageDrawable(null)

            thumbJob = scope.launch {
                val bmp = withContext(Dispatchers.IO) { ThumbnailLoader.load(resolver, item) }
                if (bmp != null) binding.imgThumb.setImageBitmap(bmp)
            }
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.1fG".format(bytes / 1073741824.0)
        bytes >= 1024 * 1024 -> "%.1fM".format(bytes / 1048576.0)
        bytes >= 1024 -> "%.0fK".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
