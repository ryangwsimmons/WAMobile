package io.github.ryangwsimmons.wamobile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_search_results.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.coroutines.*

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var actionbar: ActionBar
    private lateinit var session: WASession

    private lateinit var results: ArrayList<SearchResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        //Get the bundle from the previous activity
        val bundle: Bundle = intent.getBundleExtra("bundle")!!

        //Get the session from the previous activity
        this.session = bundle.getParcelable<WASession>("session")!!

        //Get the data from the previous activity from the bundle
        val term: String = bundle.getString("term")!!
        val subjects: ArrayList<String> = bundle.getStringArrayList("subjects")!!
        val courseLevels: ArrayList<String> = bundle.getStringArrayList("courseLevels")!!
        val courseNums: ArrayList<String> = bundle.getStringArrayList("courseNums")!!
        val sections: ArrayList<String> = bundle.getStringArrayList("sections")!!
        val times: ArrayList<String> = bundle.getStringArrayList("times")!!
        val days: ArrayList<Boolean> = ArrayList<Boolean>(bundle.getBooleanArray("days")!!.toList())
        val courseKeywords: String = bundle.getString("courseKeywords")!!
        val location: String = bundle.getString("location")!!
        val academicLevel: String = bundle.getString("academicLevel")!!
        val instructorsLastName: String = bundle.getString("instructorsLastName")!!

        //Define the toolbar for the activity
        this.toolbar = toolBar
        setSupportActionBar(toolbar)

        //Set properties for the ActionBar in the activity
        this.actionbar = supportActionBar!!
        this.actionbar.setDisplayHomeAsUpEnabled(true)

        //Set the title of the activity
        this.actionbar.title = "Search Results"

        //Set up the recycler view settings
        var adapter: SearchResultsAdapter = SearchResultsAdapter(ArrayList<SearchResult>())
        recyclerView_sections.adapter = adapter
        recyclerView_sections.layoutManager = LinearLayoutManager(this)

        //Create an error handler for the coroutine that will be executed to get the search results
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@SearchResultsActivity, error.message ?: "A network error has occurred. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(this@SearchResultsActivity, "A network error has occurred. Please check your internet connection try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the search results
        CoroutineScope(errorHandler).launch {
            //Get the list of search results
            this@SearchResultsActivity.results = this@SearchResultsActivity.session.getSearchResults(term, subjects, courseLevels, courseNums, sections, times, days, courseKeywords, location, academicLevel, instructorsLastName)

            //Update the list of items in the adapter
            withContext(Dispatchers.Main) {
                adapter.setItems(this@SearchResultsActivity.results)
                adapter.notifyItemInserted(this@SearchResultsActivity.results.size - 1)
            }
        }
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                this.finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}