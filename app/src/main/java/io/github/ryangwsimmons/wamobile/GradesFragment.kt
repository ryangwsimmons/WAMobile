package io.github.ryangwsimmons.wamobile

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_grades.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main

class GradesFragment(private var session: WASession, private var actionBar: ActionBar) : Fragment(), TermsAdapter.OnTermClickListener {

    lateinit var terms: ArrayList<Term>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Set the title for this fragment to "Select Term"
        actionBar.title = getString(R.string.termSelect_message)

        //Get the items in the fragment
        val listItems: View = inflater.inflate(R.layout.fragment_grades, container, false)

        //Get the recycler view from the fragment
        var recyclerViewTerms: RecyclerView = listItems.findViewById<RecyclerView>(R.id.recyclerView_terms)

        //Set the adapter, layout manager, and other settings for the recycler view for the terms
        var adapter: TermsAdapter = TermsAdapter(ArrayList<Term>(), this, activity!!.applicationContext)
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
            var bundle: Bundle = Bundle()
            bundle.putParcelable("session", this@GradesFragment.session)
            bundle.putParcelableArrayList("terms", this@GradesFragment.terms)
            bundle.putInt("position", position)
            putExtra("bundle", bundle)
        }

        //Start the activity
        startActivity(intent)
    }
}