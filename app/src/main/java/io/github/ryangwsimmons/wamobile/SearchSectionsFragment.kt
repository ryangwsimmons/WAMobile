package io.github.ryangwsimmons.wamobile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import kotlinx.android.synthetic.main.days_checkboxes.view.*
import kotlinx.android.synthetic.main.search_sections_filter_group.view.*
import kotlinx.coroutines.*

class SearchSectionsFragment(private var session: WASession, private var actionBar: ActionBar) : Fragment() {

    //Create attribute for the list of items in the view
    private lateinit var listItems: View

    //Create attributes for the spinner array adapters
    private lateinit var termsAdapter: ArrayAdapter<DropdownOption>
    private lateinit var subjectAdapters: ArrayList<ArrayAdapter<DropdownOption>>
    private lateinit var courseLevelAdapters: ArrayList<ArrayAdapter<DropdownOption>>
    private lateinit var timeAdapters: ArrayList<ArrayAdapter<DropdownOption>>
    private lateinit var locationAdapter: ArrayAdapter<DropdownOption>
    private lateinit var academicLevelAdapter: ArrayAdapter<DropdownOption>

    //Create attributes for the spinners and edit texts where multiple spinners have the same options
    private lateinit var subjects: ArrayList<Spinner>
    private lateinit var courseLevels: ArrayList<Spinner>
    private lateinit var courseNums: ArrayList<EditText>
    private lateinit var sections: ArrayList<EditText>
    private lateinit var times: ArrayList<Spinner>
    private lateinit var days: ArrayList<CheckBox>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Set the title of the action bar
        actionBar.title = getString(R.string.sfs_title)

        //Initialize the array adapter array lists
        this.subjectAdapters = ArrayList()
        this.courseLevelAdapters = ArrayList()
        this.timeAdapters = ArrayList()

        //Get the items in the fragment
        this.listItems = inflater.inflate(R.layout.fragment_search_sections, container, false)

        //Create an error handler for the coroutine that will be executed to get the search sections filters
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(activity!!.applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(activity!!.applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the search sections filters
        CoroutineScope(errorHandler).launch {
            //Create ArrayLists for all the dropdown options
            val terms: ArrayList<DropdownOption> = ArrayList()
            val subjects: ArrayList<DropdownOption> = ArrayList()
            val courseLevels: ArrayList<DropdownOption> = ArrayList()
            val timesList: ArrayList<DropdownOption> = ArrayList()
            val locations: ArrayList<DropdownOption> = ArrayList()
            val academicLevels: ArrayList<DropdownOption> = ArrayList()

            //Populate all the ArrayLists with the options
            this@SearchSectionsFragment.session.getSearchSectionsFilterValues(terms, subjects, courseLevels, timesList, locations, academicLevels)

            withContext(Dispatchers.Main) {
                //Initialize all the adapters and spinners
                this@SearchSectionsFragment.subjects = ArrayList()
                this@SearchSectionsFragment.courseLevels = ArrayList()
                this@SearchSectionsFragment.courseNums = ArrayList()
                this@SearchSectionsFragment.sections = ArrayList()
                this@SearchSectionsFragment.times = ArrayList()
                this@SearchSectionsFragment.days = ArrayList()

                this@SearchSectionsFragment.initializeAdapters(terms, subjects, courseLevels, timesList, locations, academicLevels)
            }
        }

        //Set up a listener for the "submit" button
        listItems.findViewById<Button>(R.id.button_submitSearch).setOnClickListener { v: View ->
            if (this.countFilledFields() < 2) {
                Toast.makeText(activity!!.applicationContext, getString(R.string.sfs_not_enough_fields), Toast.LENGTH_SHORT).show()
            } else {
                //Create an intent to start the search results activity
                val intent = Intent(activity!!.applicationContext, SearchResultsActivity::class.java).apply {
                    //Create a bundle for passing data to the new activity, and add the data to it
                    val bundle: Bundle = Bundle()

                    bundle.putParcelable("session", this@SearchSectionsFragment.session)
                    bundle.putString("term", (this@SearchSectionsFragment.listItems.findViewById<Spinner>(R.id.spinner_terms).selectedItem as DropdownOption).value)
                    bundle.putStringArrayList("subjects", ArrayList<String>(this@SearchSectionsFragment.subjects.map { subject: Spinner -> (subject.selectedItem as DropdownOption).value }))
                    bundle.putStringArrayList("courseLevels", ArrayList<String>(this@SearchSectionsFragment.courseLevels.map { courseLevel: Spinner -> (courseLevel.selectedItem as DropdownOption).value }))
                    bundle.putStringArrayList("courseNums", ArrayList<String>(this@SearchSectionsFragment.courseNums.map { courseNum: EditText -> courseNum.text.toString() }))
                    bundle.putStringArrayList("sections", ArrayList<String>(this@SearchSectionsFragment.sections.map { section: EditText -> section.text.toString() }))
                    bundle.putStringArrayList("times", ArrayList<String>(this@SearchSectionsFragment.times.map { time: Spinner -> (time.selectedItem as DropdownOption).value }))
                    bundle.putBooleanArray("days", this@SearchSectionsFragment.days.map { day: CheckBox -> day.isChecked }.toBooleanArray())
                    bundle.putString("courseKeywords", this@SearchSectionsFragment.listItems.findViewById<EditText>(R.id.editText_courseKeywords).text.toString())
                    bundle.putString("location", (this@SearchSectionsFragment.listItems.findViewById<Spinner>(R.id.spinner_location).selectedItem as DropdownOption).value)
                    bundle.putString("academicLevel", (this@SearchSectionsFragment.listItems.findViewById<Spinner>(R.id.spinner_academicLevel).selectedItem as DropdownOption).value)
                    bundle.putString("instructorsLastName", this@SearchSectionsFragment.listItems.findViewById<EditText>(R.id.editText_instructorsLastName).text.toString())

                    putExtra("bundle", bundle)
                }

                //Start the search results activity
                startActivity(intent)
            }
        }

        // Inflate the layout for this fragment
        return listItems
    }

    private fun initializeAdapters(terms: ArrayList<DropdownOption>,
                                   subjects: ArrayList<DropdownOption>,
                                   courseLevels: ArrayList<DropdownOption>,
                                   times: ArrayList<DropdownOption>,
                                   locations: ArrayList<DropdownOption>,
                                   academicLevels: ArrayList<DropdownOption>) {
        //Initialize the terms adapter and spinner
        this.termsAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, terms)
        this.listItems.findViewById<Spinner>(R.id.spinner_terms).adapter = this.termsAdapter

        //Initialize the subject adapters and spinners
        this.subjectAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, subjects))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup1).spinner_subject.adapter = this.subjectAdapters[0]
        this.subjects.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup1).spinner_subject)

        this.subjectAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, subjects))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup2).spinner_subject.adapter = this.subjectAdapters[1]
        this.subjects.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup2).spinner_subject)

        this.subjectAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, subjects))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup3).spinner_subject.adapter = this.subjectAdapters[2]
        this.subjects.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup3).spinner_subject)

        this.subjectAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, subjects))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup4).spinner_subject.adapter = this.subjectAdapters[3]
        this.subjects.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup4).spinner_subject)

        this.subjectAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, subjects))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup5).spinner_subject.adapter = this.subjectAdapters[4]
        this.subjects.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup5).spinner_subject)

        //Initialize the course level adapters and spinners
        this.courseLevelAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, courseLevels))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup1).spinner_courseLevel.adapter = this.courseLevelAdapters[0]
        this.courseLevels.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup1).spinner_courseLevel)

        this.courseLevelAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, courseLevels))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup2).spinner_courseLevel.adapter = this.courseLevelAdapters[1]
        this.courseLevels.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup2).spinner_courseLevel)

        this.courseLevelAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, courseLevels))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup3).spinner_courseLevel.adapter = this.courseLevelAdapters[2]
        this.courseLevels.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup3).spinner_courseLevel)

        this.courseLevelAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, courseLevels))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup4).spinner_courseLevel.adapter = this.courseLevelAdapters[3]
        this.courseLevels.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup4).spinner_courseLevel)

        this.courseLevelAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, courseLevels))
        this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup5).spinner_courseLevel.adapter = this.courseLevelAdapters[4]
        this.courseLevels.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup5).spinner_courseLevel)

        //Initialize the edit texts that involve course numbers
        this.courseNums.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup1).editText_courseNum)

        this.courseNums.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup2).editText_courseNum)

        this.courseNums.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup3).editText_courseNum)

        this.courseNums.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup4).editText_courseNum)

        this.courseNums.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup5).editText_courseNum)

        //Initialize the edit texts that involve course sections
        this.sections.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup1).editText_section)

        this.sections.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup2).editText_section)

        this.sections.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup3).editText_section)

        this.sections.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup4).editText_section)

        this.sections.add(this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroup5).editText_section)

        //Initialize the adapters and spinners that involve times
        this.timeAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, times))
        this.listItems.findViewById<Spinner>(R.id.spinner_meetingAfter).adapter = this.timeAdapters[0]
        this.times.add(this.listItems.findViewById(R.id.spinner_meetingAfter))

        this.timeAdapters.add(ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, times))
        this.listItems.findViewById<Spinner>(R.id.spinner_endingBefore).adapter = this.timeAdapters[1]
        this.times.add(this.listItems.findViewById(R.id.spinner_endingBefore))

        //Initialize the checkboxes for days
        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_mon)

        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_tue)

        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_wed)

        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_thu)

        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_fri)

        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_sat)

        this.days.add(this.listItems.findViewById<CheckBox>(R.id.days_checkboxes).checkbox_sun)

        //Initialize the location adapter and spinner
        this.locationAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, locations)
        this.listItems.findViewById<Spinner>(R.id.spinner_location).adapter = this.locationAdapter

        //Initialize the academic level adapter and spinner
        this.academicLevelAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, academicLevels)
        this.listItems.findViewById<Spinner>(R.id.spinner_academicLevel).adapter = this.academicLevelAdapter
    }

    private fun countFilledFields(): Int {
        //Count the number of filled fields
        var filledFieldCount = 0
        if ((listItems.findViewById<Spinner>(R.id.spinner_terms).selectedItem as DropdownOption).value != "") {
            ++filledFieldCount
        }

        for (subject: Spinner in this.subjects) {
            if ((subject.selectedItem as DropdownOption).value != "") {
                ++filledFieldCount
            }
        }

        for (courseLevel: Spinner in this.courseLevels) {
            if ((courseLevel.selectedItem as DropdownOption).value != "") {
                ++filledFieldCount
            }
        }

        for (courseNum: EditText in this.courseNums) {
            if (courseNum.text.toString() != "") {
                ++filledFieldCount
            }
        }

        for (section: EditText in this.sections) {
            if (section.text.toString() != "") {
                ++filledFieldCount
            }
        }

        for (time: Spinner in this.times) {
            if ((time.selectedItem as DropdownOption).value != "") {
                ++filledFieldCount
            }
        }

        for (day: CheckBox in this.days) {
            if (day.isChecked) {
                ++filledFieldCount
            }
        }

        if (listItems.findViewById<EditText>(R.id.editText_courseKeywords).text.toString() != "") {
            ++filledFieldCount
        }

        if ((listItems.findViewById<Spinner>(R.id.spinner_location).selectedItem as DropdownOption).value != "") {
            ++filledFieldCount
        }

        if ((listItems.findViewById<Spinner>(R.id.spinner_academicLevel).selectedItem as DropdownOption).value != "") {
            ++filledFieldCount
        }

        if (listItems.findViewById<EditText>(R.id.editText_instructorsLastName).text.toString() != "") {
            ++filledFieldCount
        }

        return filledFieldCount
    }
}