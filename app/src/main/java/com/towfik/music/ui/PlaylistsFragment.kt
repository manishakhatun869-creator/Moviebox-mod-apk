package com.towfik.music.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.towfik.music.MainActivity
import com.towfik.music.MusicApplication
import com.towfik.music.R
import com.towfik.music.data.Playlist
import com.towfik.music.databinding.FragmentPlaylistsBinding
import com.towfik.music.premium.PlaylistPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val app: MusicApplication get() = requireActivity().application as MusicApplication

    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PlaylistAdapter(onOpen = { open(it) }, onDelete = { confirmDelete(it) })
        binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylists.adapter = adapter
        binding.newPlaylist.setOnClickListener { tryCreate() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun refresh() {
        val list = app.playlists.all()
        val limit = if (app.premium.status().isPremium) null else PlaylistPolicy.FREE_PLAYLIST_LIMIT
        binding.playlistsSubtitle.text = getString(
            R.string.playlists_subtitle, list.size, limit ?: list.size
        )
        adapter.submitList(list)
        binding.playlistsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun tryCreate() {
        val isPremium = app.premium.status().isPremium
        val current = app.playlists.all().size
        if (!PlaylistPolicy.canCreatePlaylist(current, isPremium)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.playlist_limit_title)
                .setMessage(getString(R.string.playlist_limit_body, PlaylistPolicy.FREE_PLAYLIST_LIMIT))
                .setPositiveButton(R.string.upgrade_now) { _, _ ->
                    (activity as? MainActivity)?.goPremium()
                }
                .setNegativeButton(R.string.not_now, null)
                .show()
            return
        }
        promptName()
    }

    private fun promptName() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.playlist_name_hint)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlists_new)
            .setView(input)
            .setPositiveButton(R.string.playlist_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    app.playlists.create(name)
                    refresh()
                }
            }
            .setNegativeButton(R.string.playlist_cancel, null)
            .show()
    }

    private fun confirmDelete(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_delete)
            .setMessage(playlist.name)
            .setPositiveButton(R.string.playlist_delete) { _, _ ->
                app.playlists.delete(playlist.id)
                refresh()
                Toast.makeText(requireContext(), R.string.playlist_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.playlist_cancel, null)
            .show()
    }

    private fun open(playlist: Playlist) {
        viewLifecycleOwner.lifecycleScope.launch {
            val tracks = withContext(Dispatchers.IO) { app.tracks.load(requireContext()) }
            if (_binding == null) return@launch
            val byId = tracks.associateBy { it.id }
            val ordered = playlist.trackIds.mapNotNull { byId[it] }
            if (ordered.isEmpty()) {
                Toast.makeText(requireContext(), R.string.playlist_track_count, Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            (activity as? MainActivity)?.requirePlayback()?.playQueue(ordered, 0)
            (activity as? MainActivity)?.showNowPlaying()
        }
    }
}
