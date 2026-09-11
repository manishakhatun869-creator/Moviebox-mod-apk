package com.towfik.music.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.towfik.music.MainActivity
import com.towfik.music.MusicApplication
import com.towfik.music.R
import com.towfik.music.data.Track
import com.towfik.music.databinding.FragmentLibraryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val app: MusicApplication get() = requireActivity().application as MusicApplication

    private lateinit var adapter: TrackAdapter
    private var allTracks: List<Track> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TrackAdapter(onPlay = { track, pos -> play(track, pos) }, onAdd = { addToPlaylist(it) })
        binding.rvTracks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTracks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.grantPermission.setOnClickListener {
            requestPermissions(arrayOf(requiredPermission()), REQUEST_AUDIO)
        }
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { filter(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) reload()
    }

    private fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), requiredPermission()) ==
            PackageManager.PERMISSION_GRANTED

    private fun reload() {
        if (!hasPermission()) {
            binding.permissionCard.visibility = View.VISIBLE
            binding.libraryEmpty.visibility = View.GONE
            binding.rvTracks.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            return
        }
        binding.permissionCard.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val tracks = withContext(Dispatchers.IO) { app.tracks.load(requireContext()) }
            if (_binding == null) return@launch
            allTracks = tracks
            binding.librarySubtitle.text = getString(R.string.library_subtitle, tracks.size)
            applyList(tracks)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun filter(query: String) {
        val q = query.trim()
        if (q.isEmpty()) applyList(allTracks)
        else applyList(
            allTracks.filter {
                it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
            }
        )
    }

    private fun applyList(list: List<Track>) {
        adapter.submitList(list)
        val empty = list.isEmpty()
        binding.libraryEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvTracks.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun play(track: Track, pos: Int) {
        val host = activity as? MainActivity ?: return
        val list = currentListForPlayback()
        host.requirePlayback()?.playQueue(list, list.indexOf(track).coerceAtLeast(0))
        host.showNowPlaying()
    }

    private fun currentListForPlayback(): List<Track> = allTracks

    private fun addToPlaylist(track: Track) {
        val playlists = app.playlists.all()
        if (playlists.isEmpty()) {
            Toast.makeText(requireContext(), R.string.playlists_empty_title, Toast.LENGTH_SHORT).show()
            return
        }
        val names = playlists.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_add_to)
            .setItems(names) { _, which ->
                app.playlists.addTrack(playlists[which].id, track.id)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.playlist_added_to, playlists[which].name),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.playlist_cancel, null)
            .show()
    }

    companion object {
        private const val REQUEST_AUDIO = 4001
    }
}
