package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import kotlinx.coroutines.*
import org.json.JSONArray
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

        //Create an error handler for the coroutine that will be executed to get the account info
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
                this@AccountViewFragment.getAccountViewInfo(null)
            }
        }

        // Inflate the layout for this fragment
        return listItems
    }

    private fun populateTermsSpinner() {
        // Create an ArrayList of strings from the account activity JSON object
        try {
            val termsArray = this.accountActivity.getJSONArray("TermPeriodBalances")
            val terms = ArrayList<DropdownOption>()
            for (i in 0 until termsArray.length()) {
                val termObject = termsArray.getJSONObject(i)
                terms.add(DropdownOption(termObject.getString("Description"), termObject.getString("Id")))
            }

            // Create an adapter for the spinner
            val termsAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, terms)

            // Set the adapter to the terms spinner in the fragment's view
            val termsSpinner = this.listItems.findViewById<Spinner>(R.id.spinner_terms)
            termsSpinner.adapter = termsAdapter

            // Set the method to run when the spinner's value is changed
            termsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    this@AccountViewFragment.getAccountViewInfo(parent!!.getItemAtPosition(position) as DropdownOption)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // This scenario should never occur, so no need to do something here as far as I know
                }
            }
        } catch(error: JSONException) {
            Toast.makeText(activity!!.applicationContext, error.message, Toast.LENGTH_LONG).show()
            Toast.makeText(activity!!.applicationContext, getString(R.string.accountView_noTermsError), Toast.LENGTH_LONG).show()
        }
    }

    private fun getAccountViewInfo(dropDownOption: DropdownOption?) {
        //Create an error handler for the coroutine that will be executed to get the new account view info
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(activity!!.applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(activity!!.applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Start a cross-fade while the data is being retrieved, if one does not already exist
        if (dropDownOption != null) {
            crossFade(progressBar, activity!!.findViewById(R.id.fragment_container), false)
        }

        //Launch a coroutine to get the account view
        CoroutineScope(errorHandler).launch {
            if (dropDownOption == null) {
                this@AccountViewFragment.accountActivity = JSONObject(session.getAccountViewInfo(this@AccountViewFragment.ellucianCookies))
            } else {
                this@AccountViewFragment.accountActivity = JSONObject(session.getAccountViewInfo(this@AccountViewFragment.ellucianCookies, dropDownOption.value))
            }

            withContext(Dispatchers.Main) {
                crossFade(activity!!.findViewById(R.id.fragment_container), progressBar, false)

                // If this is the initial call, populate the terms spinner
                if (dropDownOption == null) {
                    this@AccountViewFragment.populateTermsSpinner()
                }

                // Filter out any transaction categories that shouldn't be shown
                val unfilteredTransactionCategories = this@AccountViewFragment.accountActivity.getJSONArray("FormulaCategories")
                val filteredTransactionCategories = JSONArray()
                for (i in 0 until unfilteredTransactionCategories.length()) {
                    val transactionCategory = unfilteredTransactionCategories.getJSONObject(i)
                    if (transactionCategory.getBoolean("IsVisible")) {
                        filteredTransactionCategories.put(transactionCategory)
                    }
                }
                // Create the list adapter, and set it as the adapter for the main ExpandableListView
                val transactionsListAdapter = AccountViewListViewAdapter(filteredTransactionCategories, activity!!.applicationContext)
                val transactionsListView: ExpandableListView = this@AccountViewFragment.listItems.findViewById(R.id.transactions_list)
                transactionsListView.setAdapter(transactionsListAdapter)
            }
        }
    }
}