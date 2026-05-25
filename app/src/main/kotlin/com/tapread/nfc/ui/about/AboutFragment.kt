package com.tapread.nfc.ui.about

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tapread.nfc.BuildConfig
import com.tapread.nfc.R
import kotlin.math.sqrt

class AboutFragment : Fragment(), SensorEventListener {

    private var tapCount = 0
    private var lastTapTime = 0L
    private var sensorManager: SensorManager? = null
    private var shakeThreshold = 12f
    private var lastShakeTime = 0L
    private val easterEggs = listOf(
        "🃏 You found a hidden card!",
        "💳 NFC goes brrr...",
        "🔮 The chip knows all...",
        "🐛 It's not a bug, it's an undocumented feature",
        "☕ This app runs on caffeine and deadlines",
        "🤖 I read cards, not minds... yet",
        "💀 deadboy was here",
        "🎰 PAN: 4242 4242 4242 4242 (just kidding)",
        "📡 *taps phone on everything*",
        "🧲 My NFC brings all the cards to the yard"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp16 = dpToPx(16)
        val dp8 = dpToPx(8)

        sensorManager = ContextCompat.getSystemService(ctx, SensorManager::class.java)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16 * 2, dp16 * 2, dp16 * 2, dp16 * 2)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        // Logo / title — tappable for easter egg
        val titleView = TextView(ctx).apply {
            text = "TapRead"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge)
            gravity = android.view.Gravity.CENTER
            setOnClickListener { onTitleTapped() }
        }
        root.addView(titleView)

        root.addView(TextView(ctx).apply {
            text = "Tap. Read. Know."
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            gravity = android.view.Gravity.CENTER
            alpha = 0.7f
            setPadding(0, dp8, 0, dp8)
        })

        root.addView(TextView(ctx).apply {
            text = "Version ${BuildConfig.VERSION_NAME}"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, dp16 * 2)
        })

        val info = """
NFC EMV card reader for fintech professionals.

Reads contactless bank cards (Visa, Mastercard, Amex, JCB, UnionPay, Discover, RuPay) and displays:

• Card number, expiry, holder name
• Track 1 & Track 2 data
• Application Identifiers (AIDs)
• ATR/ATS chip identification
• CPLC card production data
• Transaction history with cryptogram
• Contactless NFC status detection
• Tokenization detection (Apple Pay, Google Pay, Samsung Pay)
• Full APDU command log

Built on devnied/EMV-NFC-Paycard-Enrollment v3.1.0

No internet permission. All data stays on device. No ads. No analytics.

For POS sellers, terminal installers, payment gateway developers, and NFC/RFID professionals.
        """.trimIndent()

        root.addView(TextView(ctx).apply {
            text = info
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setLineSpacing(0f, 1.4f)
            setPadding(0, 0, 0, dp16 * 2)
        })

        // Made by deadboy
        root.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, dp16, 0, dp16) }
            setBackgroundColor(0x20000000)
        })

        root.addView(TextView(ctx).apply {
            text = "Made with 💀 by deadboy"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp8, 0, dp8 / 2)
        })

        root.addView(TextView(ctx).apply {
            text = "github.com/deadboy18"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.md_primary, null))
            alpha = 0.8f
            setPadding(0, 0, 0, dp16)
        })

        root.addView(TextView(ctx).apply {
            text = "Tip: Tap the title 7 times or shake your phone 👀"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            gravity = android.view.Gravity.CENTER
            alpha = 0.3f
        })

        return android.widget.ScrollView(ctx).apply { addView(root) }
    }

    private fun onTitleTapped() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 2000) tapCount = 0
        lastTapTime = now
        tapCount++

        context?.let { com.tapread.nfc.util.HapticUtil.pulse(it) }

        when (tapCount) {
            3 -> Toast.makeText(context, "Keep going...", Toast.LENGTH_SHORT).show()
            5 -> Toast.makeText(context, "Almost there...", Toast.LENGTH_SHORT).show()
            7 -> {
                // POS terminal haptic — the money shot!
                context?.let { com.tapread.nfc.util.HapticUtil.posTerminal(it) }
                val egg = easterEggs.random()
                Toast.makeText(context, egg, Toast.LENGTH_LONG).show()
                tapCount = 0
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // Shake detection for bonus easter egg
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val accel = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
        if (accel > shakeThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 2000) {
                lastShakeTime = now
                context?.let { com.tapread.nfc.util.HapticUtil.heartbeat(it) }
                Toast.makeText(context, "🃏 ${easterEggs.random()}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
    ).toInt()
}
