package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_grade_view.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.apache.commons.lang3.StringUtils

class GradeViewActivity : AppCompatActivity() {

    private lateinit var session: WASession
    private lateinit var terms: ArrayList<Term>
    private var position: Int = 0
    private lateinit var toolbar: Toolbar
    private lateinit var actionbar: ActionBar
    private lateinit var grades: ArrayList<Grade>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade_view)

        //Get the bundle from the previous activity
        val bundle = intent.getBundleExtra("bundle")!!

        //Get the session from the bundle
        this.session = bundle.getParcelable<WASession>("session")!!

        //Get the terms from the bundle
        this.terms = bundle.getParcelableArrayList<Term>("terms")!!

        //Get the position of the selected term in the ArrayList
        this.position = bundle.getInt("position")

        //Define the toolbar for the activity
        this.toolbar = toolBar
        setSupportActionBar(toolbar)

        //Set properties for the ActionBar in the activity
        this.actionbar = supportActionBar!!
        this.actionbar.setDisplayHomeAsUpEnabled(true)

        //Set the title of the activity
        this.actionbar.title = "Grades for " + this.terms.get(this.position).longName

        //Set up the recycler view settings
        var adapter: GradeViewAdapter = GradeViewAdapter(ArrayList<Grade>())
        recyclerView_grades.adapter = adapter
        recyclerView_grades.layoutManager = LinearLayoutManager(this)

        //Create an error handler for the coroutine that will be executed to get the grades
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@GradeViewActivity, error.message ?: "A network error has occurred. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(this@GradeViewActivity, "A network error has occurred. Please check your internet connection try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the grades for the signed-in user for the selected term
        CoroutineScope(errorHandler).launch {
            //Instantiate a new ArrayList to hold grades
            this@GradeViewActivity.grades = ArrayList<Grade>()
            //Create StringBuilders for advisor and term GPA
            var advisor: StringBuilder = StringBuilder()
            var termGPA: StringBuilder = StringBuilder()
            //Get a list with the grades
            this@GradeViewActivity.session.getGrades(this@GradeViewActivity.position,
                this@GradeViewActivity.terms,
                this@GradeViewActivity.grades,
                advisor,
                termGPA)

            //Update the list of items in the adapter
            withContext(Main) {
                adapter.setItems(this@GradeViewActivity.grades)
                adapter.notifyItemInserted(this@GradeViewActivity.grades.size - 1)
                textView_advisorName.text = StringUtils.abbreviate(advisor.toString(), 20)
                textView_TermGPAValue.text = termGPA.toString()
                relativeLayout_AdvisorGPA.visibility = View.VISIBLE
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