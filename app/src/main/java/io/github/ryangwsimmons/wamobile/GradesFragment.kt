package io.github.ryangwsimmons.wamobile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KFunction3

class GradesFragment(private var session: WASession, private var actionBar: ActionBar, private var progressBar: View, private var crossFade: KFunction3<View, View, Boolean, Unit>) : Fragment(), TermsAdapter.OnTermClickListener {

    private lateinit var terms: ArrayList<Term>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Set the title for this fragment to "Select Term"
        actionBar.title = getString(R.string.termSelect_message)

        //Get the items in the fragment
        val listItems: View = inflater.inflate(R.layout.fragment_grades, container, false)

        //Get the recycler view from the fragment
        val recyclerViewTerms: RecyclerView = listItems.findViewById(R.id.recyclerView_terms)

        //Set the adapter, layout manager, and other settings for the recycler view for the terms
        val adapter: TermsAdapter = TermsAdapter(ArrayList(), this, activity!!.applicationContext)
        recyclerViewTerms.adapter = adapter
        recyclerViewTerms.layoutManager = LinearLayoutManager(activity!!.applicationContext)

        //Create an error handler for the coroutine that will be executed to get the terms
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Main).launch {
                Toast.makeText(activity!!.applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(activity!!.applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the terms for the currently signed-in user
        CoroutineScope(errorHandler).launch {
            //Get an ArrayList with the term info for the user
            this@GradesFragment.terms = session.getTerms()

            withContext(Main) {
                crossFade(activity!!.findViewById(R.id.fragment_container), progressBar, false)
                adapter.setItems(this@GradesFragment.terms)
                adapter.notifyItemInserted(this@GradesFragment.terms.size - 1)
            }
        }

        // Inflate the layout for this fragment
        return listItems
    }

    override fun onTermClick(position: Int) {
        //Create an intent to change activity
        val intent = Intent(activity!!.applicationContext, GradeViewActivity::class.java).apply {
            //Set up the parcel for passing data to the activity
            val bundle: Bundle = Bundle()
            bundle.putParcelable("session", this@GradesFragment.session)
            bundle.putParcelableArrayList("terms", this@GradesFragment.terms)
            bundle.putInt("position", position)
            putExtra("bundle", bundle)
        }

        //Start the activity
        startActivity(intent)
    }
}