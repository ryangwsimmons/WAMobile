@file:Suppress("SpellCheckingInspection")

package ca.ryangwsimmons.wamobile

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationView
import ca.ryangwsimmons.wamobile.databinding.ActivityBaseBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class BaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaseBinding

    private lateinit var session: WASession
    private lateinit var toolbar: Toolbar
    private lateinit var actionBar: ActionBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fragmentManager: FragmentManager
    private lateinit var navigationView: NavigationView
    private lateinit var navListener: NavMenuItemListener
    private lateinit var progressBar: ProgressBar
    private lateinit var fragmentContainer: FrameLayout
    private var doubleBacktoExitPressedOnce: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Get the session object
        val bundle: Bundle = intent.getBundleExtra("bundle")!!
        this.session = bundle.getParcelable("session")!!

        // Define the toolbar for the activity
        this.toolbar = this.binding.layoutToolbar.toolBar
        setSupportActionBar(toolbar)

        // Get other layout items
        this.progressBar = this.binding.progressBar
        this.fragmentContainer = this.binding.fragmentContainer

        // Set properties for the ActionBar in the activity
        this.actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu)

        // Define the DrawerLayout, FragmentManager, NavMenuItemListener, and NavigationView for the activity
        this.drawerLayout = this.binding.drawerLayout
        this.fragmentManager = supportFragmentManager
        this.navListener = NavMenuItemListener(session, this.fragmentManager, this.drawerLayout, actionBar, this.progressBar, this.fragmentContainer)
        this.navigationView = this.binding.navigationView

        // Set the listeners for the NavigationView
        this.navigationView.setNavigationItemSelectedListener(this.navListener)

        // Set the default selection for the navigation drawer to "News"
        this.navListener.fragmentSwitchAction(this.navigationView.menu.getItem(0))

        // Handle errors in the coroutine that is launched after this
        val errorHandler = CoroutineExceptionHandler {_, error ->
            CoroutineScope(Main).launch {
                Toast.makeText(applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Launch a coroutine to display a message on sign in
        CoroutineScope(errorHandler).launch {
            val name = session.getName()
            withContext(Main) {
                Toast.makeText(applicationContext, getString(R.string.signedIn_message) + name, Toast.LENGTH_SHORT).show()
            }
        }

        // Add listener for the "About" option in the menu
        this.binding.menuItemAbout.setOnClickListener { v: View ->
            // Close the navigation drawer
            this.binding.drawerLayout.closeDrawer(GravityCompat.START)

            // Open the dialog after 250ms, to allow the navigation drawer's close animation to finish
            Handler(Looper.getMainLooper()).postDelayed({
                val builder = AlertDialog.Builder(this@BaseActivity)
                val viewGroup = findViewById<ViewGroup>(android.R.id.content)
                val dialogView = LayoutInflater.from(v.context).inflate(R.layout.dialog_about, viewGroup, false)

                // Set dialog values
                // Set version number
                val pInfo = this.packageManager.getPackageInfo(this.packageName, 0)
                dialogView.findViewById<TextView>(R.id.about_versionString).text = "${resources.getString(R.string.about_version_base)} ${pInfo.versionName}"

                // Set website link
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dialogView.findViewById<TextView>(R.id.textView_website).text =
                        Html.fromHtml("<a href=\"${resources.getString(R.string.about_websiteUrl)}\">${resources.getString(R.string.about_website)}</a>", Html.FROM_HTML_MODE_COMPACT)
                } else {
                    dialogView.findViewById<TextView>(R.id.textView_website).text =
                        Html.fromHtml("<a href=\"${resources.getString(R.string.about_websiteUrl)}\">${resources.getString(R.string.about_website)}</a>")
                }
                dialogView.findViewById<TextView>(R.id.textView_website).movementMethod = LinkMovementMethod.getInstance()

                // Set source code link
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dialogView.findViewById<TextView>(R.id.textView_source).text =
                        Html.fromHtml("<a href=\"${resources.getString(R.string.about_githubUrl)}\">${resources.getString(R.string.about_source)}</a>", Html.FROM_HTML_MODE_COMPACT)
                } else {
                    dialogView.findViewById<TextView>(R.id.textView_source).text =
                        Html.fromHtml("<a href=\"${resources.getString(R.string.about_githubUrl)}\">${resources.getString(R.string.about_source)}</a>")
                }
                dialogView.findViewById<TextView>(R.id.textView_source).movementMethod = LinkMovementMethod.getInstance()

                // Open the dialog
                builder.setView(dialogView)
                val alertDialog = builder.create()
                alertDialog.show()
            }, 250)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (isTaskRoot && !this.doubleBacktoExitPressedOnce) {
            this.doubleBacktoExitPressedOnce = true
            Toast.makeText(applicationContext, getString(R.string.exit_message), Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({ this.doubleBacktoExitPressedOnce = false }, 2000)
        } else {
            super.onBackPressed()
        }
    }
}