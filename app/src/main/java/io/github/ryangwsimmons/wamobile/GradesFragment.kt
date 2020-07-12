package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import kotlinx.android.synthetic.main.fragment_grades.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main

class GradesFragment(private var session: WASession, private var actionBar: ActionBar) : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Create an error handler for the coroutine that will be executed to get the terms
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Main).launch {
                Toast.makeText(activity!!.applicationContext, error.message ?: "A network error has occurred. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(activity!!.applicationContext, "A network error has occurred. Please check your internet connection try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the terms for the currently signed-in user
        CoroutineScope(errorHandler).launch {
            //Get an ArrayList with the term info for the user
            val terms: ArrayList<Term> = session.getTerms()

            withContext(Main) {
                //Create a new ArrayAdaptor for the list of terms
                val adapter = ArrayAdapter<String>(activity!!.applicationContext, R.layout.listview_grades_layout, terms.map { term: Term -> term.longName}.toTypedArray())

                //Set the terms Listview to use the new adapter
                listView_terms.adapter = adapter
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Set the title for this fragment to "Select Term"
        actionBar.title = "Select Term"

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grades, container, false)
    }

}