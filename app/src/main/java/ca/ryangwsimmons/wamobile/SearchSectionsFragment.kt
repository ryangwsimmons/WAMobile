package ca.ryangwsimmons.wamobile

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
import ca.ryangwsimmons.wamobile.databinding.FragmentSearchSectionsBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.reflect.KFunction3

class SearchSectionsFragment(private var session: WASession, private var actionBar: ActionBar, private var progressBar: View, private var crossFade: KFunction3<View, View, Boolean, Unit>) : Fragment(), OnDateSetListener, OnTimeSetListener {

    private var _binding: FragmentSearchSectionsBinding? = null
    private val binding get() = _binding!!

    // Create attributes for the spinner array adapters and text fields
    private lateinit var termsAdapter: ArrayAdapter<DropdownOption>
    private lateinit var meetingStartDate: EditText
    private lateinit var meetingEndDate: EditText
    private lateinit var subjectAdapters: ArrayList<ArrayAdapter<DropdownOption>>
    private lateinit var timeAdapter: ArrayAdapter<DropdownOption>
    private lateinit var timeStartsBy: EditText
    private lateinit var timeEndsBy: EditText
    private lateinit var locationAdapter: ArrayAdapter<DropdownOption>
    private lateinit var academicLevelAdapter: ArrayAdapter<DropdownOption>

    // Create attributes for the spinners and edit texts where multiple spinners have the same options
    private lateinit var subjects: ArrayList<Spinner>
    private lateinit var courseNums: ArrayList<EditText>
    private lateinit var sections: ArrayList<EditText>
    private lateinit var days: ArrayList<CheckBox>

    // Create attributes for the search sections options retrieved
    private lateinit var searchSectionsData: SearchSectionsData

    // Create attribute that determines the current date being selected
    private var currentDateSelect: Int = 0

    // Create attribute that determines the current time being selected
    private var currentTimeSelect: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchSectionsBinding.inflate(inflater, container, false)
        val viewModel = binding.root

        // Set the title of the action bar
        actionBar.title = getString(R.string.sfs_title)

        // Initialize the array adapter array lists
        this.subjectAdapters = ArrayList()

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
            // Populate all the ArrayLists with the options
            val data = this@SearchSectionsFragment.session.getSearchSectionsFilterValues()

            // Wrap the JSON string into a Kotlin object
            wrapData(data)

            withContext(Dispatchers.Main) {
                //Initialize all the adapters and spinners
                this@SearchSectionsFragment.subjects = ArrayList()
                this@SearchSectionsFragment.courseNums = ArrayList()
                this@SearchSectionsFragment.sections = ArrayList()
                this@SearchSectionsFragment.days = ArrayList()

                this@SearchSectionsFragment.initializeAdapters()
                crossFade(requireActivity().findViewById(R.id.fragment_container), progressBar, false)
            }
        }

        // If a term is selected, the date selects should be hidden
        this.binding.spinnerTerms.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // If the value of the currently selected item is not an empty string, hide the date selectors
                if (this@SearchSectionsFragment.termsAdapter.getItem(position)!!.value != "") {
                    this@SearchSectionsFragment.binding.rowMeetingStart.visibility = View.GONE
                    this@SearchSectionsFragment.binding.rowMeetingEnd.visibility = View.GONE
                } else {
                    this@SearchSectionsFragment.binding.rowMeetingStart.visibility = View.VISIBLE
                    this@SearchSectionsFragment.binding.rowMeetingEnd.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }

        }

        // Set up listeners for date selects
        this.binding.editTextMeetingStartDate.setOnClickListener(EditDateOnClickListener())
        this.binding.editTextMeetingStartDate.onFocusChangeListener = EditDateOnFocusChangeListener()

        this.binding.editTextMeetingEndDate.setOnClickListener(EditDateOnClickListener())
        this.binding.editTextMeetingEndDate.onFocusChangeListener = EditDateOnFocusChangeListener()

        // Set up listeners for time selects
        this.binding.editTextTimeStartsBy.setOnClickListener(EditTimeOnClickListener())
        this.binding.editTextTimeStartsBy.onFocusChangeListener = EditTimeOnFocusChangeListener()

        this.binding.editTextTimeEndsBy.setOnClickListener(EditTimeOnClickListener())
        this.binding.editTextTimeEndsBy.onFocusChangeListener = EditTimeOnFocusChangeListener()

        // Set up a listener for the "add" button
        this.binding.buttonAddFilterGroup.setOnClickListener {
            addFilterGroup(inflater)
        }

        // Set up a listener for the "submit" button
        this.binding.buttonSubmitSearch.setOnClickListener {
            submitSectionSearch()
        }

        // Inflate the layout for this fragment
        return viewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun wrapData(data: String) {
        // Parse the request JSON
        val dataJson = JSONObject(data)

        // Create a new SearchSectionsData object to hold the response data
        val searchSectionsData = SearchSectionsData()

        // Parse the subjects array to get all available subjects
        val subjectsArray = dataJson.getJSONArray("Subjects")
        for (i in 0 until subjectsArray.length()) {
            val subjectJson = subjectsArray.getJSONObject(i)
            if (subjectJson.getBoolean("ShowInCourseSearch")) {
                searchSectionsData.subjects.add(DropdownOption(subjectJson.getString("Description"), subjectJson.getString("Code")))
            }
        }

        // Parse the terms array to get all available terms
        val termsArray = dataJson.getJSONArray("Terms")
        for (i in 0 until termsArray.length()) {
            val termJson = termsArray.getJSONObject(i)
            searchSectionsData.terms.add(DropdownOption(termJson.getString("Item2"), termJson.getString("Item1")))
        }

        // Parse the locations array to get all available locations
        val locationsArray = dataJson.getJSONArray("Locations")
        for (i in 0 until locationsArray.length()) {
            val locationJson = locationsArray.getJSONObject(i)
            searchSectionsData.locations.add(DropdownOption(locationJson.getString("Item2"), locationJson.getString("Item1")))
        }

        // Parse the academic levels array to get all available academic levels
        val academicLevelsArray = dataJson.getJSONArray("AcademicLevels")
        for (i in 0 until academicLevelsArray.length()) {
            val academicLevelJson = academicLevelsArray.getJSONObject(i)
            searchSectionsData.academicLevels.add(DropdownOption(academicLevelJson.getString("Item2"), academicLevelJson.getString("Item1")))
        }

        // Parse the meeting times array to get all available meeting times
        val meetingTimesArray = dataJson.getJSONArray("TimeRanges")
        for (i in 0 until meetingTimesArray.length()) {
            val meetingTimeJson = meetingTimesArray.getJSONObject(i)
            val beginningTime = meetingTimeJson.getInt("Item2")
            val endTime = meetingTimeJson.getInt("Item3")
            searchSectionsData.meetingTimes.add(DropdownOption(
                meetingTimeJson.getString("Item1"),
                "$beginningTime,$endTime"
            ))
        }

        this.searchSectionsData = searchSectionsData
    }

    private fun initializeAdapters() {
        // Initialize the terms adapter and spinner
        this.termsAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.terms)
        this.binding.spinnerTerms.adapter = this.termsAdapter

        // Initialize the meeting text fields
        this.meetingStartDate = this.binding.editTextMeetingStartDate
        this.meetingEndDate = this.binding.editTextMeetingEndDate

        // Initialize all of the filter groups
        val filterGroups = this.binding.searchSectionsFilterGroups
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

        // Initialize the time-related fields
        this.timeAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.meetingTimes)
        this.binding.spinnerTimeOfDay.adapter = this.timeAdapter

        this.timeStartsBy = this.binding.editTextTimeStartsBy
        this.timeEndsBy = this.binding.editTextTimeEndsBy

        // Initialize the checkboxes for days
        val daysCheckboxes = this.binding.daysCheckboxes.root
        for (i in 0 until daysCheckboxes.childCount) {
            val checkboxLayout = daysCheckboxes.getChildAt(i) as LinearLayout
            val checkBox = checkboxLayout.getChildAt(0) as CheckBox

            this.days.add(checkBox)
        }

        // Initialize the location adapter and spinner
        this.locationAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.locations)
        this.binding.spinnerLocation.adapter = this.locationAdapter

        // Initialize the academic level adapter and spinner
        this.academicLevelAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, this.searchSectionsData.academicLevels)
        this.binding.spinnerAcademicLevel.adapter = this.academicLevelAdapter
    }

    override fun onDateSet(
        view: DatePickerDialog?,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int
    ) {
        val editText = this.binding.root.findViewById<EditText>(this.currentDateSelect)

        editText.text = SpannableStringBuilder("%02d/%02d/%d".format(monthOfYear + 1, dayOfMonth, year))
    }

    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        val editText = this.binding.root.findViewById<EditText>(this.currentTimeSelect)

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
        val filterGroups = this.binding.searchSectionsFilterGroups

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

    private fun submitSectionSearch() {
        // Create an intent to start the search results activity
        val intent = Intent(requireActivity().applicationContext, SearchResultsActivity::class.java).apply {
            //Create a bundle for passing data to the new activity, and add the data to it
            val bundle = Bundle()

            bundle.putParcelable("session", this@SearchSectionsFragment.session)
            bundle.putString("term", (this@SearchSectionsFragment.binding.spinnerTerms.selectedItem as DropdownOption).value)
            bundle.putString("meetingStartDate", this@SearchSectionsFragment.meetingStartDate.text.toString())
            bundle.putString("meetingEndDate", this@SearchSectionsFragment.meetingEndDate.text.toString())
            bundle.putStringArrayList("subjects", ArrayList<String>(this@SearchSectionsFragment.subjects.map { subject: Spinner -> (subject.selectedItem as DropdownOption).value }))
            bundle.putStringArrayList("courseNums", ArrayList<String>(this@SearchSectionsFragment.courseNums.map { courseNum: EditText -> courseNum.text.toString() }))
            bundle.putStringArrayList("sections", ArrayList<String>(this@SearchSectionsFragment.sections.map { section: EditText -> section.text.toString() }))
            bundle.putString("timeOfDay", (this@SearchSectionsFragment.binding.spinnerTimeOfDay.selectedItem as DropdownOption).value)
            bundle.putString("timeStartsBy", this@SearchSectionsFragment.timeStartsBy.text.toString())
            bundle.putString("timeEndsBy", this@SearchSectionsFragment.timeEndsBy.text.toString())
            bundle.putBooleanArray("days", this@SearchSectionsFragment.days.map { day: CheckBox -> day.isChecked }.toBooleanArray())
            bundle.putString("location", (this@SearchSectionsFragment.binding.spinnerLocation.selectedItem as DropdownOption).value)
            bundle.putString("academicLevel", (this@SearchSectionsFragment.binding.spinnerAcademicLevel.selectedItem as DropdownOption).value)

            putExtra("bundle", bundle)
        }

        // Start the search results activity
        startActivity(intent)
    }
}