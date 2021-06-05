package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import kotlin.reflect.KFunction3

class AccountViewFragment(private var session: WASession, private var actionBar: ActionBar, private var progressBar: View, private var crossFade: KFunction3<View, View, Boolean, Unit>) : Fragment() {

    // Cookies for the Ellucian portal where the account activity page is located
    private lateinit var ellucianCookies: MutableMap<String, String>

    // The JSON object containing all account activity info for the current term
    private lateinit var accountActivity: JSONObject

    // The items in the fragment, allows content to be updated later if necessary
    private lateinit var listItems: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Set the title of the action bar
        actionBar.title = getString(R.string.accountView_title)

        //Get the items in the fragment
        this.listItems = inflater.inflate(R.layout.fragment_account_view, container, false)

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
            this@AccountViewFragment.ellucianCookies = session.getEllucianCookies()
            this@AccountViewFragment.accountActivity = JSONObject(session.getAccountViewInfo(this@AccountViewFragment.ellucianCookies))

            withContext(Dispatchers.Main) {
                crossFade(activity!!.findViewById(R.id.fragment_container), progressBar, false)
                this@AccountViewFragment.populateTermsSpinner()
            }
        }

        // Inflate the layout for this fragment
        return listItems
    }

    private fun populateTermsSpinner(){
        // Create an ArrayList of strings from the account activity JSON object
        try {
            val termsArray = this.accountActivity.getJSONArray("TermPeriodBalances")
            val terms = ArrayList<String>()
            for (i in 0 until termsArray.length()) {
                val termObject = termsArray.getJSONObject(i)
                terms.add(termObject.getString("Description"))
            }

            // Create an adapter for the spinner
            val termsAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, terms)

            // Set the adapter to the terms spinner in the fragment's view
            this.listItems.findViewById<Spinner>(R.id.spinner_terms).adapter = termsAdapter
        } catch(error: JSONException) {
            Toast.makeText(activity!!.applicationContext, error.message, Toast.LENGTH_LONG).show()
            Toast.makeText(activity!!.applicationContext, getString(R.string.accountView_noTermsError), Toast.LENGTH_LONG).show()
        }
    }
}