package com.towfik.music.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.towfik.music.BuildConfig
import com.towfik.music.MainActivity
import com.towfik.music.MusicApplication
import com.towfik.music.R
import com.towfik.music.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val app: MusicApplication get() = requireActivity().application as MusicApplication

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.versionText.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)

        binding.rowEqualizer.setOnClickListener { gated { pick("Equalizer", arrayOf("Flat", "Bass Boost", "Vocal", "Treble")) } }
        binding.rowSpeed.setOnClickListener { gated { pick("Playback speed", arrayOf("0.75x", "1x", "1.25x", "1.5x")) } }
        binding.rowSleep.setOnClickListener { gated { pickSleep() } }
        binding.rowTheme.setOnClickListener { gated { pick("Accent theme", arrayOf("Violet", "Gold", "Teal")) } }
    }

    override fun onResume() {
        super.onResume()
        val premium = app.premium.status().isPremium
        val vis = if (premium) View.GONE else View.VISIBLE
        binding.lockEqualizer.visibility = vis
        binding.lockSpeed.visibility = vis
        binding.lockSleep.visibility = vis
        binding.lockTheme.visibility = vis
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun gated(action: () -> Unit) {
        if (app.premium.status().isPremium) {
            action()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.premium_locked_title)
                .setMessage(R.string.premium_locked_body)
                .setPositiveButton(R.string.upgrade_now) { _, _ ->
                    (activity as? MainActivity)?.goPremium()
                }
                .setNegativeButton(R.string.not_now, null)
                .show()
        }
    }

    private fun pick(title: String, options: Array<String>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(options) { _, which ->
                Toast.makeText(requireContext(), options[which], Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun pickSleep() {
        val options = arrayOf(
            getString(R.string.settings_sleep_off),
            getString(R.string.settings_sleep_set, 15),
            getString(R.string.settings_sleep_set, 30),
            getString(R.string.settings_sleep_set, 60)
        )
        pick(getString(R.string.settings_sleep_timer), options)
    }
}
