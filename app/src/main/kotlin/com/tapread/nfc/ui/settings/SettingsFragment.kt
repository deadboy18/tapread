package com.tapread.nfc.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.tapread.nfc.R
import com.tapread.nfc.ui.CardsViewModel

class SettingsFragment : Fragment() {

    private val viewModel: CardsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp16 = dpToPx(16)
        val dp12 = dpToPx(12)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp16)
        }

        // Title
        root.addView(sectionTitle("Settings", true))

        // ── Appearance ──
        root.addView(sectionLabel("Appearance"))

        root.addView(switchRow("Dark Mode", "Override system theme",
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        ) { checked ->
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            viewModel.getStorage().darkMode = checked
        })

        // ── Privacy ──
        root.addView(sectionLabel("Privacy"))

        root.addView(switchRow("Mask card numbers", "Show only first 4 and last 4 digits",
            viewModel.maskPan
        ) { checked ->
            viewModel.setMaskPan(checked)
        })

        // ── Data ──
        root.addView(sectionLabel("Data"))

        // Export JSON
        root.addView(MaterialButton(ctx).apply {
            text = "Export scanned data (JSON)"
            setOnClickListener { exportJson() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp12, 0, 0) }
        })

        // Clear with confirmation
        root.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Clear all scanned cards"
            setTextColor(resources.getColor(R.color.status_err, null))
            setOnClickListener { confirmClear() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp12, 0, 0) }
        })

        // Scan count
        val count = viewModel.scans.value?.size ?: 0
        root.addView(TextView(ctx).apply {
            text = "$count card(s) stored"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            alpha = 0.6f
            setPadding(0, dpToPx(8), 0, 0)
        })

        return android.widget.ScrollView(ctx).apply { addView(root) }
    }

    private fun exportJson() {
        val json = viewModel.exportJson()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "TapRead Card Data Export")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        startActivity(Intent.createChooser(intent, "Export card data"))
    }

    private fun confirmClear() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear all scanned cards?")
            .setMessage("This will permanently delete all stored card data. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.clearScans()
                Snackbar.make(requireView(), "All scans cleared", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── UI Helpers ──

    private fun sectionTitle(text: String, isMain: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextAppearance(
                if (isMain) com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
                else com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
            )
            setPadding(0, 0, 0, dpToPx(16))
        }
    }

    private fun sectionLabel(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            setTextColor(resources.getColor(R.color.md_primary, null))
            setPadding(0, dpToPx(20), 0, dpToPx(4))
        }
    }

    private fun switchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(12), 0, dpToPx(12))

            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(ctx).apply {
                    text = title
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                })
                addView(TextView(ctx).apply {
                    text = subtitle
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                    alpha = 0.7f
                })
            })

            addView(Switch(ctx).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
            })
        }
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
    ).toInt()
}
