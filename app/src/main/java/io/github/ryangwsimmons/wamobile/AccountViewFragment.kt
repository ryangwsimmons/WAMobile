package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import io.github.ryangwsimmons.wamobile.databinding.FragmentAccountViewBinding
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KFunction3

class AccountViewFragment(private var session: WASession, private var actionBar: ActionBar, private var progressBar: View, private var crossFade: KFunction3<View, View, Boolean, Unit>) : Fragment() {

    // Cookies for the Ellucian portal where the account activity page is located
    private lateinit var ellucianCookies: MutableMap<String, String>

    // The currently selected term in the terms spinner
    private lateinit var currentTerm: AccountViewTerm

    // A list of all the terms available in the account view spinner
    private val allTerms: ArrayList<AccountViewTerm> = ArrayList<AccountViewTerm>()

    // The items in the fragment, allows content to be updated later if necessary
    private lateinit var viewModel: View

    private var _binding: FragmentAccountViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountViewBinding.inflate(inflater, container, false)
        val viewModel = binding.root

        //Set the title of the action bar
        actionBar.title = getString(R.string.accountView_title)

        //Create an error handler for the coroutine that will be executed to get the account info
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(requireActivity().applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(requireActivity().applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the account view
        CoroutineScope(errorHandler).launch {
            this@AccountViewFragment.ellucianCookies = session.getEllucianCookies()

            withContext(Dispatchers.Main) {
                this@AccountViewFragment.getAccountViewInfo(null)
            }
        }

        // Inflate the layout for this fragment
        return viewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getAccountViewInfo(dropDownOption: DropdownOption?) {
        //Create an error handler for the coroutine that will be executed to get the new account view info
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(requireActivity().applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(requireActivity().applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Start a cross-fade while the data is being retrieved, if one does not already exist
        if (dropDownOption != null) {
            crossFade(progressBar, requireActivity().findViewById(R.id.fragment_container), false)
        }

        //Launch a coroutine to get the account view
        CoroutineScope(errorHandler).launch {
            val accountActivity: JSONObject = if (dropDownOption == null) {
                JSONObject(session.getAccountViewInfo(this@AccountViewFragment.ellucianCookies))
            } else {
                JSONObject(session.getAccountViewInfo(this@AccountViewFragment.ellucianCookies, dropDownOption.value))
            }

            withContext(Dispatchers.Main) {
                crossFade(requireActivity().findViewById(R.id.fragment_container), progressBar, false)

                // Wrap the JSON data to native Kotlin objects
                this@AccountViewFragment.wrapData(accountActivity)

                // If this is the initial call, populate the terms spinner
                if (dropDownOption == null) {
                    this@AccountViewFragment.populateTermsSpinner()
                }

                // Set the balance
                val balanceTextView = this@AccountViewFragment.binding.textViewBalance
                val balanceFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                balanceFormat.currency = Currency.getInstance("CAD")
                balanceTextView.text = balanceFormat.format(this@AccountViewFragment.currentTerm.balance)

                // Create the list adapter, and set it as the adapter for the main ExpandableListView
                val transactionsListAdapter = AccountViewListViewAdapter(this@AccountViewFragment.currentTerm.categories, requireActivity().applicationContext)
                val transactionsListView: ExpandableListView = this@AccountViewFragment.binding.transactionsList
                transactionsListView.setAdapter(transactionsListAdapter)
            }
        }
    }

    // This function wraps the JSON data from the account view in native Kotlin objects
    // This will make it easier to fix in the future if the JSON data structure ever changes
    private fun wrapData(jsonData: JSONObject) {
        // Populate the list of terms
        val termsArray = jsonData.getJSONArray("TermPeriodBalances")

        // Populate the data object with the JSON data
        for (i in 0 until termsArray.length()) {
            val termObject = termsArray.getJSONObject(i)
            this.allTerms.add(AccountViewTerm(termObject.getString("Id"), termObject.getString("Description"), termObject.getDouble("Balance")))
        }

        // Create an object for the currently selected term
        this.currentTerm = AccountViewTerm(jsonData.getString("DisplayTermPeriodCode"), jsonData.getString("DisplayTermPeriodDescription"), jsonData.getDouble("Balance"))

        // Loop through each category, adding the data to the account view term object
        val categoriesArray = jsonData.getJSONArray("FormulaCategories")
        for (i in 0 until categoriesArray.length()) {
            // Get the category JSON object
            val categoryJson = categoriesArray.getJSONObject(i)

            // Create the category Kotlin object, if it should be visible
            if (categoryJson.getBoolean("IsVisible")) {
                val categoryObject = AccountViewCategory(categoryJson.getString("Description"), categoryJson.getDouble("Amount"))

                // Loop through each transaction in the category
                val transactionsArray = categoryJson.getJSONArray("Transactions")
                for (j in 0 until transactionsArray.length()) {
                    // Get the transaction JSON object
                    val transactionJson = transactionsArray.getJSONObject(j)

                    // Create the transaction Kotlin object, if it should be visible
                    if (!transactionJson.has("IsVisible") || (transactionJson.has("IsVisible") && transactionJson.getBoolean("IsVisible"))) {
                        val transactionName = when {
                            transactionJson.has("Description") -> transactionJson.getString("Description")
                            transactionJson.has("Name") -> transactionJson.getString("Name")
                            transactionJson.has("PaymentMethodCode") -> transactionJson.getString("PaymentMethodCode")
                            transactionJson.has("TypeDescription") -> transactionJson.getString(("TypeDescription"))
                            else -> "Unknown Transaction Item $j"
                        }
                        val transactionObject = AccountViewTransaction(
                            transactionName,
                            transactionJson.getDouble("Amount")
                        )

                        // Loop through each charge details array in the transaction, if such an array exists
                        if (transactionJson.has("ChargeDetails")) {
                            val chargeDetailsArray = transactionJson.getJSONArray("ChargeDetails")
                            for (k in 0 until chargeDetailsArray.length()) {
                                val chargeDetailsJson = chargeDetailsArray.getJSONObject(k)

                                // Create the charge details Kotlin object
                                val chargeName = when {
                                    chargeDetailsJson.has("Name") -> chargeDetailsJson.getString("Name")
                                    chargeDetailsJson.has("Description") -> chargeDetailsJson.getString(
                                        "Description"
                                    )
                                    else -> "Unknown Charge Item $k"
                                }
                                val chargeDetailsObject = AccountViewChargeDetails(
                                    chargeName,
                                    chargeDetailsJson.getDouble("Amount")
                                )

                                // Add the charge to the transactions object
                                transactionObject.chargeDetails.add(chargeDetailsObject)
                            }
                        }

                        // Add the transaction to the category object
                        categoryObject.transactions.add(transactionObject)
                    }
                }

                // Add the category object to the term object
                this.currentTerm.categories.add(categoryObject)
            }
        }
    }

    private fun populateTermsSpinner() {
        // Create an ArrayList of strings from the account activity JSON object
        try {
            val terms = ArrayList<DropdownOption>()
            for (term in this.allTerms) {
                terms.add(DropdownOption(term.name, term.id))
            }

            // Create an adapter for the spinner
            val termsAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, terms)

            // Set the adapter to the terms spinner in the fragment's view
            val termsSpinner = this.binding.spinnerTerms
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
            Toast.makeText(requireActivity().applicationContext, error.message, Toast.LENGTH_LONG).show()
            Toast.makeText(requireActivity().applicationContext, getString(R.string.accountView_noTermsError), Toast.LENGTH_LONG).show()
        }
    }
}