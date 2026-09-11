package com.towfik.music.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.towfik.music.data.Track
import com.towfik.music.databinding.ItemTrackBinding
import com.towfik.music.util.Duration

class TrackAdapter(
    private val onPlay: (Track, Int) -> Unit,
    private val onAdd: (Track) -> Unit
) : RecyclerView.Adapter<TrackAdapter.VH>() {

    private val items = mutableListOf<Track>()

    fun submitList(list: List<Track>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(private val binding: ItemTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: Track) {
            binding.title.text = track.title
            binding.artist.text = track.artist.ifBlank {
                binding.root.context.getString(com.towfik.music.R.string.unknown_artist)
            }
            binding.duration.text = Duration.format(track.durationMs)
            binding.trackRow.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onPlay(items[pos], pos)
            }
            binding.addBtn.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onAdd(items[pos])
            }
        }
    }
}
