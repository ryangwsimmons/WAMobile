package ca.ryangwsimmons.wamobile

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import ca.ryangwsimmons.wamobile.databinding.ActivitySectionDetailsBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.Exception

class SectionDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySectionDetailsBinding

    private lateinit var toolbar: Toolbar
    private lateinit var actionbar: ActionBar

    private lateinit var sectionDetails: SectionDetails

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySectionDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Define the toolbar for the activity
        this.toolbar = this.binding.layoutToolbar.toolBar
        setSupportActionBar(toolbar)

        // Set properties for the ActionBar in the activity
        this.actionbar = supportActionBar!!
        this.actionbar.setDisplayHomeAsUpEnabled(true)

        //Set the title of the activity
        this.actionbar.title = getString(R.string.secdet_title)

        // Get data from previous activity
        val bundle: Bundle = intent.getBundleExtra("bundle")!!

        val session: WASession = bundle.getParcelable("session")!!
        val result: SearchResult = bundle.getParcelable("result")!!
        @Suppress("UNCHECKED_CAST")
        val cookies = intent.extras!!.getSerializable("cookies") as HashMap<String, String>
        val reqVerToken = bundle.getString("reqVerToken")!!

        // Create an error handler for the coroutine that will be executed to get the section details
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@SectionDetailsActivity, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(this@SectionDetailsActivity, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        // Launch a coroutine to get the section details
        CoroutineScope(errorHandler).launch {
            // Get the section details
            val detailsString = session.getSectionDetails(result.sectionId, cookies, reqVerToken)

            // Build the SectionDetails object from the result object, as well as JSON values obtained from the request to the API
            wrapData(detailsString, result)

            //Update the fields in the UI, and make them visible
            withContext(Dispatchers.Main) {
                this@SectionDetailsActivity.binding.textViewSectionDetailsTitle.text = this@SectionDetailsActivity.sectionDetails.title
                this@SectionDetailsActivity.binding.textViewSectionDetailsDescription.text = this@SectionDetailsActivity.sectionDetails.description

                this@SectionDetailsActivity.binding.textViewSectionDetailsFacultyName.text = this@SectionDetailsActivity.sectionDetails.facultyName

                if (this@SectionDetailsActivity.sectionDetails.facultyEmail != "") {
                    // Create a SpannableString and ClickableSpan to enable the email to be clicked like a link, and for an action to happen when that click occurs
                    val emailSpan = SpannableString(this@SectionDetailsActivity.sectionDetails.facultyEmail)
                    val emailClick: ClickableSpan = object: ClickableSpan() {
                        override fun onClick(view: View) {
                            // When an email is clicked on, create an intent that opens an email client
                            val intent: Intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("mailto:?to=" + Uri.encode(this@SectionDetailsActivity.sectionDetails.facultyEmail))
                            }

                            // Start a new activity, allowing the user to send an email using any of the clients they've installed
                            try {
                                startActivity(Intent.createChooser(intent, getString(R.string.secdet_send_email_prompt)))
                            } catch(e: Exception) {
                                Toast.makeText(this@SectionDetailsActivity, getString(R.string.secdet_email_unable), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    // Set the span of the SpannableString to the ClickableSpan which triggers the email client chooser
                    emailSpan.setSpan(emailClick, 0, this@SectionDetailsActivity.sectionDetails.facultyEmail.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // Set the text of the email TextView to the SpannableString
                    this@SectionDetailsActivity.binding.textViewSectionDetailsFacultyEmail.text = emailSpan

                    // Enable links to work for the email TextView
                    this@SectionDetailsActivity.binding.textViewSectionDetailsFacultyEmail.movementMethod = LinkMovementMethod.getInstance()

                    // Make the email field visible
                    this@SectionDetailsActivity.binding.sectionDetailsEmailContainer.visibility = View.VISIBLE
                }

                if (this@SectionDetailsActivity.sectionDetails.facultyPhone != "") {
                    // Create a SpannableString and ClickableSpan to enable the phone number to be clicked like a link, and for an action to happen when that click occurs
                    val phoneSpan = SpannableString(this@SectionDetailsActivity.sectionDetails.facultyPhone)
                    val phoneClick: ClickableSpan = object: ClickableSpan() {
                        override fun onClick(view: View) {
                            // When a phone number is clicked on, create an intent that opens the dialer
                            val intent: Intent = Intent(Intent.ACTION_DIAL).apply {
                                // Remove all characters except the numbers from the phone number
                                data = Uri.parse("tel:" + Uri.encode(this@SectionDetailsActivity.sectionDetails.facultyPhone))
                            }

                            // Start a new activity, allowing the user to call the number using their dialer
                            try {
                                startActivity(intent)
                            } catch(e: Exception) {
                                Toast.makeText(this@SectionDetailsActivity, getString(R.string.secdet_phone_unable), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    // Set the span of the SpannableString to the ClickableSpan which triggers the dialer opening
                    phoneSpan.setSpan(phoneClick, 0, this@SectionDetailsActivity.sectionDetails.facultyPhone.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // Set the text of the phone TextView to the SpannableString
                    this@SectionDetailsActivity.binding.textViewSectionDetailsFacultyPhone.text = phoneSpan

                    // Enable links to work for the phone TextView
                    this@SectionDetailsActivity.binding.textViewSectionDetailsFacultyPhone.movementMethod = LinkMovementMethod.getInstance()

                    // Make the phone field visible
                    this@SectionDetailsActivity.binding.sectionDetailsPhoneContainer.visibility = View.VISIBLE
                }

                // For each additional course spec, inflate the layout, add the values, then add the layout to the end of the existing LinearLayout in the activity
                val content = this@SectionDetailsActivity.binding.linearLayoutSectionsDetailsContent
                for ((name, value) in this@SectionDetailsActivity.sectionDetails.courseSpecs) {
                    val courseSpec = layoutInflater.inflate(R.layout.sectiondetails_coursespec, content, false)

                    courseSpec.findViewById<TextView>(R.id.textView_sectionDetails_specLabel).text = name
                    courseSpec.findViewById<TextView>(R.id.textView_sectionDetails_specValue).text = value

                    content.addView(courseSpec)
                }

                // Hide the progress bar
                this@SectionDetailsActivity.binding.progressBarSectionDetailsWrapper.visibility = View.GONE

                // Show the section details
                this@SectionDetailsActivity.binding.scrollViewSectionDetailsContent.visibility = View.VISIBLE
            }
        }
    }

    private fun wrapData(detailsString: String, result: SearchResult) {
        // Create a JSON object from the details string returned by the API
        val detailsJson = JSONObject(detailsString)

        // Get the course title from JSON
        val title = detailsJson.getString("Title")

        val description = getCourseDescription(result.unparsedDescriptionString)

        val facultyName = if (detailsJson.getJSONArray("InstructorItems").length() > 0) {
            detailsJson.getJSONArray("InstructorItems").getJSONObject(0).getString("Name")
        } else {
            "TBD"
        }

        val facultyEmail = if (detailsJson.getJSONArray("InstructorItems").length() > 0) {
            if (detailsJson.getJSONArray("InstructorItems").getJSONObject(0).getJSONArray("EmailAddresses").length() > 0) {
                detailsJson.getJSONArray("InstructorItems").getJSONObject(0).getJSONArray("EmailAddresses").getString(0)
            } else {
                ""
            }
        } else {
            ""
        }

        val facultyPhone = if (detailsJson.getJSONArray("InstructorItems").length() > 0) {
            if (detailsJson.getJSONArray("InstructorItems").getJSONObject(0).getJSONArray("PhoneNumbers").length() > 0) {
                detailsJson.getJSONArray("InstructorItems").getJSONObject(0).getJSONArray("PhoneNumbers").getString(0)
            } else {
                ""
            }
        } else {
            ""
        }

        val courseSpecs = getCourseSpecs(result.unparsedDescriptionString)

        this.sectionDetails = SectionDetails(title, description, facultyName, facultyEmail, facultyPhone, courseSpecs)
    }

    private fun getCourseDescription(rawString: String): String {
        // Define the Regex object with the pattern used to retrieve the course description from the JSON string
        val regex = Regex(".+?(?=\\[)")

        // Use regex to get the course description from the JSON string
        val description = regex.find(rawString)!!.groupValues[0]

        // Return the course description string, with the random double spaces (why?) and leading, trailing whitespace removed
        return description.replace("  ", " ").trim()
    }

    private fun getCourseSpecs(unparsedDescriptionString: String): HashMap<String, String> {
        // Define Regex object with pattern for getting the different course specs in the string
        val regex = Regex("\\[(.+?)]")

        // Use regex to get the list of matches in the string
        val matches = regex.findAll(unparsedDescriptionString).toList()

        // Use map to turn the list of matches into a list of strings, one for each course spec
        val specStrings = matches.map{match: MatchResult -> match.groupValues[1].replace("  ", " ")}

        // Define a HashMap that holds key pair values for each course spec
        val courseSpecs = HashMap<String, String>()

        // Go through each spec string, and separate the name of the spec from it's value, adding each to the hashmap
        for (specString in specStrings) {
            val specStringSplitList = specString.split(": ")
            val key = specStringSplitList[0] + ":"
            val value = specStringSplitList[1].trim()

            courseSpecs[key] = value
        }

        // Return the hashmap of paired specs
        return courseSpecs
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
}