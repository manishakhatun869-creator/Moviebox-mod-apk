package com.towfik.music.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.android.billingclient.api.ProductDetails
import com.towfik.music.MusicApplication
import com.towfik.music.R
import com.towfik.music.databinding.FragmentPremiumBinding
import com.towfik.music.premium.BillingManager
import com.towfik.music.premium.Feature
import com.towfik.music.premium.Plan

class PremiumFragment : Fragment() {

    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding!!
    private val app: MusicApplication get() = requireActivity().application as MusicApplication

    private val callback = object : BillingManager.Callback {
        override fun onPlans(plans: Map<Plan, ProductDetails>) {
            if (_binding == null) return
            binding.priceMonthly.text = price(plans[Plan.MONTHLY]) ?: getString(R.string.premium_per_month)
            binding.priceYearly.text = price(plans[Plan.YEARLY]) ?: getString(R.string.premium_per_year)
            binding.priceLifetime.text = price(plans[Plan.LIFETIME]) ?: getString(R.string.premium_once)
        }

        override fun onPurchaseSucceeded(plan: Plan) {
            if (_binding == null) return
            refreshStatus()
            Toast.makeText(requireContext(), R.string.premium_active_title, Toast.LENGTH_SHORT).show()
        }

        override fun onPurchaseCancelled() {
            if (_binding == null) return
            Toast.makeText(requireContext(), R.string.premium_purchase_cancelled, Toast.LENGTH_SHORT).show()
        }

        override fun onPurchaseFailed() {
            if (_binding == null) return
            Toast.makeText(requireContext(), R.string.premium_purchase_failed, Toast.LENGTH_SHORT).show()
        }

        override fun onRestoreDone(found: Boolean) {
            if (_binding == null) return
            refreshStatus()
            Toast.makeText(
                requireContext(),
                if (found) R.string.premium_restored else R.string.premium_nothing_to_restore,
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onBillingUnavailable() {
            if (_binding == null) return
            Toast.makeText(requireContext(), R.string.premium_billing_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildFeatureList()
        binding.buyMonthly.setOnClickListener { app.billing.purchase(requireActivity(), Plan.MONTHLY, callback) }
        binding.buyYearly.setOnClickListener { app.billing.purchase(requireActivity(), Plan.YEARLY, callback) }
        binding.buyLifetime.setOnClickListener { app.billing.purchase(requireActivity(), Plan.LIFETIME, callback) }
        binding.restorePurchases.setOnClickListener { app.billing.restore(callback) }
        binding.manageSubscription.setOnClickListener {
            runCatching {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        app.billing.connect(callback)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildFeatureList() {
        val labels = mapOf(
            Feature.UNLIMITED_PLAYLISTS to R.string.premium_feature_playlists,
            Feature.EQUALIZER to R.string.premium_feature_eq,
            Feature.PLAYBACK_SPEED to R.string.premium_feature_speed,
            Feature.ACCENT_THEMES to R.string.premium_feature_themes,
            Feature.SLEEP_TIMER to R.string.premium_feature_sleep,
            Feature.GAPLESS_QUEUE to R.string.premium_feature_gapless
        )
        binding.featuresList.removeAllViews()
        labels.forEach { (feature, res) ->
            val tv = AppCompatTextView(requireContext()).apply {
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
                compoundDrawablePadding = 24
                setPadding(0, 12, 0, 12)
                setText(res)
                setTextColor(resources.getColor(R.color.on_surface, null))
                textSize = 16f
            }
            binding.featuresList.addView(
                tv,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun refreshStatus() {
        val isPremium = app.premium.status().isPremium
        binding.activeCard.visibility = if (isPremium) View.VISIBLE else View.GONE
        (requireActivity() as? com.towfik.music.MainActivity)?.refreshPremiumBadge()
    }

    private fun price(details: ProductDetails?): String? = runCatching {
        details ?: return null
        if (details.productType == com.android.billingclient.api.BillingClient.ProductType.INAPP) {
            details.oneTimePurchasePriceFormatted
        } else {
            details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice
        }
    }.getOrNull()
}
