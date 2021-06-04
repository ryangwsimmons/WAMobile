package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import kotlinx.coroutines.*

class AccountViewFragment(private var session: WASession, private var actionBar: ActionBar) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Set the title of the action bar
        actionBar.title = getString(R.string.accountView_title)

        //Create an error handler for the coroutine that will be executed to get the search sections filters
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(activity!!.applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(activity!!.applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the account view
        CoroutineScope(errorHandler).launch {
            session.getAccountViewInfo()

            withContext(Dispatchers.Main) {

            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account_view, container, false)
    }
}