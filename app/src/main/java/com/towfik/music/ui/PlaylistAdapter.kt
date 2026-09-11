package com.towfik.music.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.towfik.music.R
import com.towfik.music.data.Playlist
import com.towfik.music.databinding.ItemPlaylistBinding

class PlaylistAdapter(
    private val onOpen: (Playlist) -> Unit,
    private val onDelete: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

    private val items = mutableListOf<Playlist>()

    fun submitList(list: List<Playlist>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist) {
            binding.playlistName.text = playlist.name
            binding.playlistCount.text = binding.root.context.getString(
                R.string.playlist_track_count, playlist.trackIds.size
            )
            binding.playlistCard.setOnClickListener { onOpen(playlist) }
            binding.deletePlaylist.setOnClickListener { onDelete(playlist) }
        }
    }
}
