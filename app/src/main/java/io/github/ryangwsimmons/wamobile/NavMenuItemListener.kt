package io.github.ryangwsimmons.wamobile

import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView

class NavMenuItemListener(private val session: WASession,
                          private val fragmentManager: FragmentManager,
                          private val drawerLayout: DrawerLayout,
                          private var actionBar: ActionBar): NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //Do something based on which item was selected
        when(item.itemId) {
            R.id.menuItem_news -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, NewsFragment(this.session, this.actionBar))
                fragmentTransaction.commit()

                this.drawerLayout.closeDrawers()
                return true
            }

            R.id.menuItem_grades -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, GradesFragment(this.session, this.actionBar))
                fragmentTransaction.commit()

                this.drawerLayout.closeDrawers()
                return true
            }

            R.id.menuItem_searchSections -> {
                item.isChecked = true

                val fragmentTransaction: FragmentTransaction = this.fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentHolder, SearchSectionsFragment(this.session, this.actionBar))
                fragmentTransaction.commit()

                this.drawerLayout.closeDrawers()
                return true
            }
        }

        return true
    }
}