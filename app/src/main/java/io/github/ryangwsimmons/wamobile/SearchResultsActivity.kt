package io.github.ryangwsimmons.wamobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.ryangwsimmons.wamobile.databinding.ActivitySearchResultsBinding
import kotlinx.coroutines.*
import org.json.JSONObject

class SearchResultsActivity : AppCompatActivity(), SearchResultsAdapter.OnSectionClickListener {

    private lateinit var binding: ActivitySearchResultsBinding

    private lateinit var toolbar: Toolbar
    private lateinit var actionbar: ActionBar
    private lateinit var session: WASession

    // Define attributes for the search filter options
    private lateinit var term: String
    private lateinit var meetingStartDate: String
    private lateinit var meetingEndDate: String
    private lateinit var subjects: ArrayList<String>
    private lateinit var courseNums: ArrayList<String>
    private lateinit var sections: ArrayList<String>
    private lateinit var timeOfDay: String
    private lateinit var timeStartsBy: String
    private lateinit var timeEndsBy: String
    private lateinit var days: ArrayList<Boolean>
    private lateinit var location: String
    private lateinit var academicLevel: String

    private lateinit var cookies: MutableMap<String, String>
    private lateinit var reqVerToken: String
    private var results: ArrayList<SearchResult> = ArrayList<SearchResult>()

    private var totalPages = -1
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Get the bundle from the previous activity
        val bundle: Bundle = intent.getBundleExtra("bundle")!!

        // Get the session from the previous activity
        this.session = bundle.getParcelable("session")!!

        // Get the data from the previous activity from the bundle
        this.term = bundle.getString("term")!!
        this.meetingStartDate = bundle.getString("meetingStartDate")!!
        this.meetingEndDate = bundle.getString("meetingEndDate")!!
        this.subjects = bundle.getStringArrayList("subjects")!!
        this.courseNums = bundle.getStringArrayList("courseNums")!!
        this.sections = bundle.getStringArrayList("sections")!!
        this.timeOfDay = bundle.getString("timeOfDay")!!
        this.timeStartsBy = bundle.getString("timeStartsBy")!!
        this.timeEndsBy = bundle.getString("timeEndsBy")!!
        this.days = ArrayList(bundle.getBooleanArray("days")!!.toList())
        this.location = bundle.getString("location")!!
        this.academicLevel = bundle.getString("academicLevel")!!

        // Define the toolbar for the activity
        this.toolbar = this.binding.layoutToolbar.toolBar
        setSupportActionBar(toolbar)

        // Set properties for the ActionBar in the activity
        this.actionbar = supportActionBar!!
        this.actionbar.setDisplayHomeAsUpEnabled(true)

        // Set the title of the activity
        this.actionbar.title = getString(R.string.searchResults_title)

        // Set up the recycler view settings
        val adapter = SearchResultsAdapter(ArrayList(), this)
        this.binding.recyclerViewSections.adapter = adapter
        this.binding.recyclerViewSections.layoutManager = LinearLayoutManager(this)

        // Create an error handler for the coroutine that will be executed to get the search results
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@SearchResultsActivity, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(this@SearchResultsActivity, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Launch a coroutine to get the search results
        CoroutineScope(errorHandler).launch {
            // Get the list of search results
            val results = this@SearchResultsActivity.session.getSearchResults(
                term,
                meetingStartDate,
                meetingEndDate,
                subjects,
                courseNums,
                sections,
                timeOfDay,
                timeStartsBy,
                timeEndsBy,
                days,
                location,
                academicLevel,
            )

            wrapData(results)

            //Update the list of items in the adapter
            withContext(Dispatchers.Main) {
                // If there are no sections, display the "No Sections Found" message and exit
                if (this@SearchResultsActivity.results.size == 0) {
                    this@SearchResultsActivity.binding.progressBarNewPage.visibility = View.GONE
                    this@SearchResultsActivity.binding.textViewNoSectionsFound.visibility = View.VISIBLE
                }

                adapter.setItems(this@SearchResultsActivity.results)
                adapter.notifyItemInserted(this@SearchResultsActivity.results.size - 1)
            }
        }

        // Set up listener to fetch new data when the user has scrolled to the bottom
        this.binding.nestedScrollViewSections.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                fetchNewPage()
            }
        })
    }

    private fun wrapData(data: SearchResultsData) {
        // Place the cookies and request verification token into their own attributes
        this.cookies = data.cookies
        this.reqVerToken = data.reqVerToken

        // Parse the results JSON into an ArrayList of SearchResult objects
        val dataJson = JSONObject(data.resultsJson)

        // If it hasn't already been set, set the total number of pages
        if (this.totalPages == -1) {
            this.totalPages = dataJson.getInt("TotalPages")
        }

        // Loop over each section from the results
        val sectionsArray = dataJson.getJSONArray("Sections")
        for (i in 0 until sectionsArray.length()) {
            // Get the current section object
            val sectionJson = sectionsArray.getJSONObject(i)

            // Get the section term
            val term = sectionJson.getJSONObject("Term").getString("Description")

            // Get the section status
            val status = sectionJson.getString("AvailabilityStatusDisplay")

            // Get the section title
            val title = "${sectionJson.getString("SectionNameDisplay")}\n${sectionJson.getString("SectionTitleDisplay")}"

            // Get the section location
            val location = sectionJson.getString("LocationDisplay")

            // Loop through all the meetings, adding them to the meetings ArrayList
            val meetings = ArrayList<String>()
            val meetingsArray = sectionJson.getJSONArray("FormattedMeetingTimes")
            for (j in 0 until meetingsArray.length()) {
                val meetingJson = meetingsArray.getJSONObject(j)

                val firstLine = if (meetingJson.getString("DaysOfWeekDisplay") == "" && meetingJson.getString("StartTimeDisplay") == "") {
                    ""
                } else {
                    "${meetingJson.getString("DaysOfWeekDisplay")} ${meetingJson.getString("StartTimeDisplay")} - ${meetingJson.getString("EndTimeDisplay")}"
                }

                val secondLine = meetingJson.getString("DatesDisplay")

                val thirdLine = "${if (meetingJson.getString("BuildingDisplay") == "") "TBD" else meetingJson.getString("BuildingDisplay") + " "}${meetingJson.getString("RoomDisplay")} (${meetingJson.getString("InstructionalMethodDisplay")})"

                meetings.add("${if (firstLine == "") "" else "$firstLine\n"}${secondLine}\n${thirdLine}\n\n")
            }

            // Get the section faculty
            val faculty = if (sectionJson.getJSONArray("FacultyDisplay").length() > 0) {
                sectionJson.getJSONArray("FacultyDisplay").getString(0)
            } else {
                ""
            }

            // Get the availability string
            val available = (if (sectionJson.isNull("Available")) "0" else sectionJson.getInt("Available").toString()) +
                    " / " + (if (sectionJson.isNull("Capacity")) "0" else sectionJson.getInt("Capacity").toString())

            // Get the credits string
            val credits = sectionJson.getJSONObject("Course").getDouble("MinimumCredits").toString()

            // Get the academic level string
            val academicLevel = sectionJson.getString("AcademicLevel")

            // Create the SearchResult object and add it to the results ArrayList
            this.results.add(SearchResult(
                term,
                status,
                title,
                location,
                meetings,
                faculty,
                available,
                credits,
                academicLevel
            ))
        }
    }

    override fun onSectionClick(position: Int) {
        //Create an intent to change activity
        val intent = Intent(this, SectionDetailsActivity::class.java).apply {
            //Set up bundle for passing data into the next activity
            val bundle = Bundle()
            bundle.putParcelable("session", this@SearchResultsActivity.session)
            bundle.putParcelable("result", this@SearchResultsActivity.results[position])
            putExtra("bundle", bundle)
        }

        //Start the activity
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                this.finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun fetchNewPage() {
        // Increment the page number
        this.currentPage++

        // If the current page is greater than the total number of pages, hide the progress bar and do nothing
        if (this.currentPage > this.totalPages) {
            this.binding.progressBarNewPage.visibility = View.GONE
            return
        }

        // Make the loading new page progress bar visible
        this.binding.progressBarNewPage.visibility = View.VISIBLE

        // Fetch the new data

        // Create an error handler for the coroutine that will be executed to get the search results
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@SearchResultsActivity, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(this@SearchResultsActivity, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Launch a coroutine to get the search results
        CoroutineScope(errorHandler).launch {
            // Get the list of search results
            val results = this@SearchResultsActivity.session.getSearchResults(
                term,
                meetingStartDate,
                meetingEndDate,
                subjects,
                courseNums,
                sections,
                timeOfDay,
                timeStartsBy,
                timeEndsBy,
                days,
                location,
                academicLevel,
                this@SearchResultsActivity.currentPage,
                this@SearchResultsActivity.cookies,
                this@SearchResultsActivity.reqVerToken
            )

            wrapData(results)

            //Update the list of items in the adapter
            withContext(Dispatchers.Main) {
                val newAdapter = SearchResultsAdapter(this@SearchResultsActivity.results, this@SearchResultsActivity)
                this@SearchResultsActivity.binding.recyclerViewSections.layoutManager = LinearLayoutManager(this@SearchResultsActivity)
                this@SearchResultsActivity.binding.recyclerViewSections.adapter = newAdapter
            }
        }
    }
}