package io.github.ryangwsimmons.wamobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView

class NavMenuItemListener(private val session: WASession,
                          private val fragmentManager: FragmentManager,
                          private val drawerLayout: DrawerLayout,
                          private var actionBar: ActionBar,
                          private var progressBar: ProgressBar,
                          private var fragmentContainer: FrameLayout): NavigationView.OnNavigationItemSelectedListener {

    //The following two functions were heavily influenced by the following Stack Overflow answer: https://stackoverflow.com/a/47841218,
    //which is licensed under CC BY-SA 3.0 (https://creativecommons.org/licenses/by-sa/3.0/).
    //Several modifications were made to the code from this answer, however.
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //Show progress bar and hide fragment container
        crossFade(progressBar, fragmentContainer, false)

        //Set the action bar to a blank title while the new fragment is loading
        actionBar.title = ""

        //Do something based on which item was selected
        drawerLayout.addDrawerListener(object: DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}


            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerClosed(drawerView: View) {
                fragmentSwitchAction(item, this)
            }
        })

        //Closes the navigation drawer, which triggers the above listener
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    //Function to do the animation between fragments
    private fun crossFade(viewIn: View, viewOut: View, animateViewOut: Boolean = true) {
        val crossFadeDuration = 200L

        //Set content view to 0% opacity but visible, so that it is visible but fully transparent during the animation
        viewIn.alpha = 0f
        viewIn.visibility = View.VISIBLE
        viewIn.bringToFront()

        //Animate the in view to 100% opacity, and clear any animation
        viewIn.animate().alpha(1f).setDuration(crossFadeDuration).setListener(null)

        //Animate the out view to 0% opacity.
        //After the animation ends, set its visibility to GONE for optimization purposes
        viewOut.animate().alpha(0f).setDuration(if (animateViewOut) crossFadeDuration else 0).setListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                viewOut.visibility = View.GONE
            }
        })
    }

    fun fragmentSwitchAction(item: MenuItem, listener: DrawerLayout.DrawerListener? = null) {
        when(item.itemId) {
            R.id.menuItem_news -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this@NavMenuItemListener.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, NewsFragment(this@NavMenuItemListener.session, this@NavMenuItemListener.actionBar))
                fragmentTransaction.commit()
            }

            R.id.menuItem_grades -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this@NavMenuItemListener.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, GradesFragment(this@NavMenuItemListener.session, this@NavMenuItemListener.actionBar))
                fragmentTransaction.commit()
            }

            R.id.menuItem_searchSections -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this@NavMenuItemListener.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, SearchSectionsFragment(this@NavMenuItemListener.session, this@NavMenuItemListener.actionBar))
                fragmentTransaction.commit()
            }

            R.id.menuItem_accountView -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this@NavMenuItemListener.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, AccountViewFragment(this@NavMenuItemListener.session, this@NavMenuItemListener.actionBar))
                fragmentTransaction.commit()
            }
        }

        //Cross fade back to the fragment container, hide the progress bar
        crossFade(fragmentContainer, progressBar, false)

        //Remove the listener so that other events besides selecting another item do not call it again
        if (listener != null) {
            drawerLayout.removeDrawerListener(listener)
        }
    }
}