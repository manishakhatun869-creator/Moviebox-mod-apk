package com.towfik.music.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.towfik.music.MainActivity
import com.towfik.music.R
import com.towfik.music.databinding.FragmentNowPlayingBinding
import com.towfik.music.playback.PlaybackService
import com.towfik.music.util.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NowPlayingFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private var service: PlaybackService? = null
    private var dragging = false
    private var shuffleOn = false
    private var repeatOn = false

    private val listener: (PlaybackService.UiState) -> Unit = { state ->
        _binding?.let { render(state) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        service = (activity as? MainActivity)?.requirePlayback()

        binding.btnPlayPause.setOnClickListener { service?.toggle() }
        binding.btnNext.setOnClickListener { service?.next(auto = false) }
        binding.btnPrev.setOnClickListener { service?.previous() }
        binding.btnShuffle.setOnClickListener {
            shuffleOn = !shuffleOn
            service?.setShuffle(shuffleOn)
            tint(binding.btnShuffle, shuffleOn)
        }
        binding.btnRepeat.setOnClickListener {
            repeatOn = !repeatOn
            service?.setRepeat(repeatOn)
            tint(binding.btnRepeat, repeatOn)
        }
        binding.seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar?) { dragging = true }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                dragging = false
                service?.seekTo(sb?.progress?.toLong() ?: 0L)
            }
        })

        service?.addListener(listener)
        startPolling()
    }

    override fun onDestroyView() {
        service?.removeListener(listener)
        _binding = null
        super.onDestroyView()
    }

    private fun tint(image: android.widget.ImageView, on: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (on) R.color.primary else R.color.on_surface_muted
        )
        image.imageTintList = ColorStateList.valueOf(color)
    }

    private fun render(state: PlaybackService.UiState) {
        val track = state.current ?: return
        binding.npTitle.text = track.title
        binding.npArtist.text = track.artist
        binding.seek.max = track.durationMs.toInt()
        binding.timeRemaining.text = "-" + Duration.format(track.durationMs)
        binding.btnPlayPause.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun startPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val b = _binding
                if (b != null && !dragging) {
                    val pos = service?.currentPositionMs() ?: 0L
                    b.seek.progress = pos.toInt()
                    b.timeElapsed.text = Duration.format(pos)
                }
                delay(500)
            }
        }
    }
}
