package com.tapread.nfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.tapread.nfc.databinding.ActivityMainBinding
import com.tapread.nfc.nfc.EmvReader
import com.tapread.nfc.nfc.NfcDispatcher
import com.tapread.nfc.ui.CardsViewModel
import com.tapread.nfc.ui.about.AboutFragment
import com.tapread.nfc.ui.home.HomeFragment
import com.tapread.nfc.ui.settings.SettingsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val log = LoggerFactory.getLogger("MainActivity")
    private lateinit var binding: ActivityMainBinding
    private lateinit var nfcDispatcher: NfcDispatcher
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val viewModel: CardsViewModel by viewModels()
    private val emvReader = EmvReader()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var readingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar + drawer
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        drawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        nfcDispatcher = NfcDispatcher(this) { tag -> onTagDiscovered(tag) }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment(), "home")
                .commit()
            binding.navView.setCheckedItem(R.id.nav_cards)
        }

        checkNfcStatus()

        // Defer NFC intent handling to after layout is ready
        if (intent?.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            binding.root.post { handleNfcIntent(intent) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent?.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                // Clear the intent action to prevent re-processing
                intent.action = ""
                onTagDiscovered(tag)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcDispatcher.isNfcAvailable && nfcDispatcher.isNfcEnabled) {
            nfcDispatcher.enableReaderMode()
        } else if (nfcDispatcher.isNfcAvailable) {
            promptEnableNfc()
        }
    }

    override fun onPause() {
        super.onPause()
        nfcDispatcher.disableReaderMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        readingDialog?.dismiss()
        scope.cancel()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // ── Navigation drawer ──

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_cards -> {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment(), "home")
                    .commit()
            }
            R.id.nav_settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment(), "settings")
                    .addToBackStack("settings")
                    .commit()
            }
            R.id.nav_about -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AboutFragment(), "about")
                    .addToBackStack("about")
                    .commit()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // ── NFC Status ──

    private fun checkNfcStatus() {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        when {
            adapter == null -> {
                AlertDialog.Builder(this)
                    .setTitle("NFC Not Available")
                    .setMessage("This device does not have NFC hardware.\nTapRead requires NFC to read bank cards.")
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show()
            }
            !adapter.isEnabled -> promptEnableNfc()
        }
    }

    private fun promptEnableNfc() {
        AlertDialog.Builder(this)
            .setTitle("NFC is Disabled")
            .setMessage("NFC is turned off. Would you like to open NFC settings?")
            .setPositiveButton("Open Settings") { _, _ ->
                try { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                catch (_: Exception) { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    // ── Reading Dialog ──

    private fun showReadingDialog() {
        runOnUiThread {
            readingDialog?.dismiss()
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_reading, null)
            readingDialog = MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create()
            readingDialog?.show()
        }
    }

    private fun dismissReadingDialog() {
        runOnUiThread { readingDialog?.dismiss(); readingDialog = null }
    }

    // ── Tag Discovery ──

    private fun onTagDiscovered(tag: Tag) {
        val isoDep = NfcDispatcher.getIsoDep(tag)
        if (isoDep == null) {
            val uid = tag.id?.let { com.tapread.nfc.util.HexUtil.toHex(it) } ?: "unknown"
            runOnUiThread {
                com.tapread.nfc.util.HapticUtil.error(this)
                Snackbar.make(binding.mainContent, "Unsupported card type. UID: $uid", Snackbar.LENGTH_LONG).show()
            }
            return
        }

        // Haptic pulse on card detect
        runOnUiThread { com.tapread.nfc.util.HapticUtil.pulse(this) }
        showReadingDialog()

        scope.launch {
            val result = withContext(Dispatchers.IO) { emvReader.read(isoDep) }
            dismissReadingDialog()
            viewModel.addScan(result)

            // Haptic success or error
            if (result.error != null) {
                com.tapread.nfc.util.HapticUtil.error(this@MainActivity)
            } else {
                com.tapread.nfc.util.HapticUtil.success(this@MainActivity)
            }

            val message = if (result.error != null) "Read: ${result.error}"
                else "Read: ${result.displayLabel}"
            Snackbar.make(binding.mainContent, message, Snackbar.LENGTH_SHORT).show()

            // Navigate to home if not already there
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment !is HomeFragment) {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment(), "home")
                    .commit()
            }
        }
    }
}
