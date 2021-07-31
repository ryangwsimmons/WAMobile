package io.github.ryangwsimmons.wamobile

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog.OnTimeSetListener
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import kotlinx.android.synthetic.main.days_checkboxes.view.*
import kotlinx.android.synthetic.main.search_sections_filter_group.view.*
import kotlinx.coroutines.*
import kotlin.reflect.KFunction3

class SearchSectionsFragment(private var session: WASession, private var actionBar: ActionBar, private var progressBar: View, private var crossFade: KFunction3<View, View, Boolean, Unit>) : Fragment(), OnDateSetListener, OnTimeSetListener {

    // Create attribute for the list of items in the view
    private lateinit var listItems: View

    // Create attributes for the spinner array adapters
    private lateinit var termsAdapter: ArrayAdapter<DropdownOption>
    private lateinit var subjectAdapters: ArrayList<ArrayAdapter<DropdownOption>>
    private lateinit var courseLevelAdapters: ArrayList<ArrayAdapter<DropdownOption>>
    private lateinit var timeAdapter: ArrayAdapter<DropdownOption>
    private lateinit var locationAdapter: ArrayAdapter<DropdownOption>
    private lateinit var academicLevelAdapter: ArrayAdapter<DropdownOption>

    // Create attributes for the spinners and edit texts where multiple spinners have the same options
    private lateinit var subjects: ArrayList<Spinner>
    private lateinit var courseLevels: ArrayList<Spinner>
    private lateinit var courseNums: ArrayList<EditText>
    private lateinit var sections: ArrayList<EditText>
    private lateinit var times: ArrayList<Spinner>
    private lateinit var days: ArrayList<CheckBox>

    // Create attributes for the search sections options retrieved
    private lateinit var searchSectionsData: SearchSectionsData

    // Create attribute that determines the current date being selected
    private var currentDateSelect: Int = 0

    // Create attribute that determines the current time being selected
    private var currentTimeSelect: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Set the title of the action bar
        actionBar.title = getString(R.string.sfs_title)

        // Initialize the array adapter array lists
        this.subjectAdapters = ArrayList()
        this.courseLevelAdapters = ArrayList()

        // Get the items in the fragment
        this.listItems = inflater.inflate(R.layout.fragment_search_sections, container, false)

        //Create an error handler for the coroutine that will be executed to get the search sections filters
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(requireActivity().applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(requireActivity().applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Launch a coroutine to get the search sections options
        CoroutineScope(errorHandler).launch {
            //Populate all the ArrayLists with the options
            this@SearchSectionsFragment.searchSectionsData = this@SearchSectionsFragment.session.getSearchSectionsFilterValues()

            withContext(Dispatchers.Main) {
                //Initialize all the adapters and spinners
                this@SearchSectionsFragment.subjects = ArrayList()
                this@SearchSectionsFragment.courseLevels = ArrayList()
                this@SearchSectionsFragment.courseNums = ArrayList()
                this@SearchSectionsFragment.sections = ArrayList()
                this@SearchSectionsFragment.times = ArrayList()
                this@SearchSectionsFragment.days = ArrayList()

                this@SearchSectionsFragment.initializeAdapters()
                crossFade(requireActivity().findViewById(R.id.fragment_container), progressBar, false)
            }
        }

        // Set up listeners for date selects
        listItems.findViewById<EditText>(R.id.editText_meetingStartDate).setOnClickListener(EditDateOnClickListener())
        listItems.findViewById<EditText>(R.id.editText_meetingStartDate).onFocusChangeListener = EditDateOnFocusChangeListener()

        listItems.findViewById<EditText>(R.id.editText_meetingEndDate).setOnClickListener(EditDateOnClickListener())
        listItems.findViewById<EditText>(R.id.editText_meetingEndDate).onFocusChangeListener = EditDateOnFocusChangeListener()

        // Set up listeners for time selects
        listItems.findViewById<EditText>(R.id.editText_timeStartsBy).setOnClickListener(EditTimeOnClickListener())
        listItems.findViewById<EditText>(R.id.editText_timeStartsBy).onFocusChangeListener = EditTimeOnFocusChangeListener()

        listItems.findViewById<EditText>(R.id.editText_timeEndsBy).setOnClickListener(EditTimeOnClickListener())
        listItems.findViewById<EditText>(R.id.editText_timeEndsBy).onFocusChangeListener = EditTimeOnFocusChangeListener()

        // Set up a listener for the "add" button
        listItems.findViewById<Button>(R.id.button_addFilterGroup).setOnClickListener {
            addFilterGroup(inflater)
        }

        // Set up a listener for the "submit" button
        listItems.findViewById<Button>(R.id.button_submitSearch).setOnClickListener {
            // Create an intent to start the search results activity
            val intent = Intent(requireActivity().applicationContext, SearchResultsActivity::class.java).apply {
                //Create a bundle for passing data to the new activity, and add the data to it
                val bundle = Bundle()

                bundle.putParcelable("session", this@SearchSectionsFragment.session)
                bundle.putString("term", (this@SearchSectionsFragment.listItems.findViewById<Spinner>(R.id.spinner_terms).selectedItem as DropdownOption).value)
                bundle.putStringArrayList("subjects", ArrayList<String>(this@SearchSectionsFragment.subjects.map { subject: Spinner -> (subject.selectedItem as DropdownOption).value }))
                bundle.putStringArrayList("courseLevels", ArrayList<String>(this@SearchSectionsFragment.courseLevels.map { courseLevel: Spinner -> (courseLevel.selectedItem as DropdownOption).value }))
                bundle.putStringArrayList("courseNums", ArrayList<String>(this@SearchSectionsFragment.courseNums.map { courseNum: EditText -> courseNum.text.toString() }))
                bundle.putStringArrayList("sections", ArrayList<String>(this@SearchSectionsFragment.sections.map { section: EditText -> section.text.toString() }))
                bundle.putStringArrayList("times", ArrayList<String>(this@SearchSectionsFragment.times.map { time: Spinner -> (time.selectedItem as DropdownOption).value }))
                bundle.putBooleanArray("days", this@SearchSectionsFragment.days.map { day: CheckBox -> day.isChecked }.toBooleanArray())
                bundle.putString("location", (this@SearchSectionsFragment.listItems.findViewById<Spinner>(R.id.spinner_location).selectedItem as DropdownOption).value)
                bundle.putString("academicLevel", (this@SearchSectionsFragment.listItems.findViewById<Spinner>(R.id.spinner_academicLevel).selectedItem as DropdownOption).value)

                putExtra("bundle", bundle)
            }

            // Start the search results activity
            startActivity(intent)
        }

        // Inflate the layout for this fragment
        return listItems
    }

    private fun initializeAdapters() {
        // Initialize the terms adapter and spinner
        this.termsAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.terms)
        this.listItems.findViewById<Spinner>(R.id.spinner_terms).adapter = this.termsAdapter

        // Initialize all of the filter groups
        val filterGroups = this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroups)
        for(i in 0 until filterGroups.childCount) {
            val filterGroup = filterGroups.getChildAt(i)

            // Initialize subjects spinner
            val subjectsAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.subjects)
            val subjectsSpinner = filterGroup.findViewById<Spinner>(R.id.spinner_subject)
            subjectsSpinner.adapter = subjectsAdapter
            this.subjects.add(subjectsSpinner)

            // Initialize course number text field
            val courseNumEditText = filterGroup.findViewById<EditText>(R.id.editText_courseNum)
            this.courseNums.add(courseNumEditText)

            // Initialize section text field
            val courseSectionEditText = filterGroup.findViewById<EditText>(R.id.editText_section)
            this.sections.add(courseSectionEditText)
        }

        this.timeAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.meetingTimes)
        this.listItems.findViewById<Spinner>(R.id.spinner_timeOfDay).adapter = this.timeAdapter
        this.times.add(this.listItems.findViewById(R.id.spinner_timeOfDay))

        // Initialize the checkboxes for days
        val daysCheckboxes = this.listItems.findViewById<LinearLayout>(R.id.days_checkboxes)
        for (i in 0 until daysCheckboxes.childCount) {
            val checkboxLayout = daysCheckboxes.getChildAt(i) as LinearLayout
            val checkBox = checkboxLayout.getChildAt(0) as CheckBox

            this.days.add(checkBox)
        }

        // Initialize the location adapter and spinner
        this.locationAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.locations)
        this.listItems.findViewById<Spinner>(R.id.spinner_location).adapter = this.locationAdapter

        // Initialize the academic level adapter and spinner
        this.academicLevelAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.academicLevels)
        this.listItems.findViewById<Spinner>(R.id.spinner_academicLevel).adapter = this.academicLevelAdapter
    }

    override fun onDateSet(
        view: DatePickerDialog?,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int
    ) {
        val editText = this@SearchSectionsFragment.listItems.findViewById<EditText>(this.currentDateSelect)

        editText.text = SpannableStringBuilder("%02d/%02d/%d".format(monthOfYear + 1, dayOfMonth, year))
    }

    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        val editText = this@SearchSectionsFragment.listItems.findViewById<EditText>(this.currentTimeSelect)

        val twelveHourAdjustedHour: Int
        val amOrPm: String

        if (hourOfDay >= 12) {
            twelveHourAdjustedHour = if (hourOfDay > 12) {
                hourOfDay - 12
            } else {
                hourOfDay
            }

            amOrPm = "PM"
        } else {
            twelveHourAdjustedHour = if (hourOfDay == 0) {
                12
            } else {
                hourOfDay
            }
            amOrPm = "AM"
        }

        editText.text = SpannableStringBuilder("%02d:%02d %s".format(twelveHourAdjustedHour, minute, amOrPm))
    }

    private inner class EditDateOnClickListener: View.OnClickListener {
        override fun onClick(v: View?) {
            this@SearchSectionsFragment.currentDateSelect = v!!.id
            val datePickerDialog = DatePickerDialog.newInstance(this@SearchSectionsFragment)
            datePickerDialog.show(this@SearchSectionsFragment.parentFragmentManager, "DatePickerDialog")
        }
    }

    private inner class EditDateOnFocusChangeListener: View.OnFocusChangeListener {
        override fun onFocusChange(v: View?, hasFocus: Boolean) {
            if (hasFocus) {
                this@SearchSectionsFragment.currentDateSelect = v!!.id
                val datePickerDialog = DatePickerDialog.newInstance(this@SearchSectionsFragment)
                datePickerDialog.show(this@SearchSectionsFragment.parentFragmentManager, "DatePickerDialog")
            }
        }
    }

    private inner class EditTimeOnClickListener: View.OnClickListener {
        override fun onClick(v: View?) {
            this@SearchSectionsFragment.currentTimeSelect = v!!.id
            val timePickerDialog = TimePickerDialog.newInstance(this@SearchSectionsFragment, false)
            timePickerDialog.show(this@SearchSectionsFragment.parentFragmentManager, "TimePickerDialog")
        }
    }

    private inner class EditTimeOnFocusChangeListener: View.OnFocusChangeListener {
        override fun onFocusChange(v: View?, hasFocus: Boolean) {
            if (hasFocus) {
                this@SearchSectionsFragment.currentTimeSelect = v!!.id
                val timePickerDialog = TimePickerDialog.newInstance(this@SearchSectionsFragment, false)
                timePickerDialog.show(this@SearchSectionsFragment.parentFragmentManager, "TimePickerDialog")
            }
        }
    }

    private fun addFilterGroup(inflater: LayoutInflater) {
        // Get the filter groups view
        val filterGroups = this.listItems.findViewById<LinearLayout>(R.id.searchSections_filterGroups)

        // Get the filter groups layout
        val filterGroup = inflater.inflate(R.layout.search_sections_filter_group, filterGroups, false)

        // Create an object for the layout params
        fun dpToPx(dp: Int): Int = (dp * Resources.getSystem().displayMetrics.density).toInt()
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(25), dpToPx(20), dpToPx(25), 0)

        // Set the filter group params
        filterGroup.layoutParams = layoutParams

        // Add the subjects data to the spinner and initialize it
        val subjectsAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.subjects)
        val subjectsSpinner = filterGroup.findViewById<Spinner>(R.id.spinner_subject)
        subjectsSpinner.adapter = subjectsAdapter
        this.subjects.add(subjectsSpinner)

        // Initialize the course number text field
        val courseNumEditText = filterGroup.findViewById<EditText>(R.id.editText_courseNum)
        this.courseNums.add(courseNumEditText)

        // Initialize the section text field
        val sectionNumEditText = filterGroup.findViewById<EditText>(R.id.editText_section)
        this.sections.add(sectionNumEditText)

        // Add the filter group to the filter groups view
        filterGroups.addView(filterGroup)
    }
}