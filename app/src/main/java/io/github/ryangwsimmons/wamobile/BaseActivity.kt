package io.github.ryangwsimmons.wamobile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BaseActivity : AppCompatActivity() {

    private lateinit var session: WASession
    private lateinit var toolbar: Toolbar
    private lateinit var actionBar: ActionBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fragmentManager: FragmentManager
    private lateinit var navigationView: NavigationView
    private lateinit var navListener: NavMenuItemListener
    private var doubleBacktoExitPressedOnce: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        //Get the session object
        this.session = getIntent().getSerializableExtra("session") as WASession

        //Define the toolbar for the activity
        this.toolbar = toolBar
        setSupportActionBar(toolbar)

        //Set properties for the ActionBar in the activity
        this.actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu)

        //Define the DrawerLayout, FragmentManager, NavMenuItemListener, and NavigationView for the activity
        this.drawerLayout = drawer_layout
        this.fragmentManager = supportFragmentManager
        this.navListener = NavMenuItemListener(session, this.fragmentManager, this.drawerLayout, actionBar)
        this.navigationView = navigation_view

        //Set the listeners for the NavigationView
        this.navigationView.setNavigationItemSelectedListener(this.navListener)

        //Set the default selection for the navigation drawer to "News"
        this.navListener.onNavigationItemSelected(navigationView.menu.getItem(0))

        //Handle errors in the coroutine that is launched after this
        val errorHandler = CoroutineExceptionHandler {_, error ->
            CoroutineScope(Main).launch {
                Toast.makeText(applicationContext, error.message ?: "A network error has occurred. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(applicationContext, "An error has occurred. Please check your internet connection try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to display a message on sign in
        CoroutineScope(errorHandler).launch {
            val name = session.getName()
            withContext(Main) {
                Toast.makeText(applicationContext, "Signed into WebAdvisor as " + name, Toast.LENGTH_SHORT).show()
            }
        }
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    public override fun onBackPressed() {
        if (isTaskRoot() && !this.doubleBacktoExitPressedOnce) {
            this.doubleBacktoExitPressedOnce = true
            Toast.makeText(applicationContext, "Press back again to exit", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed(Runnable { this.doubleBacktoExitPressedOnce = false }, 2000)
        } else {
            super.onBackPressed()
        }
    }
}