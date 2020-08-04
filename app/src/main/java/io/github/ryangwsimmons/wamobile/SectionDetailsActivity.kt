package io.github.ryangwsimmons.wamobile

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
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_section_details.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.coroutines.*
import kotlin.Exception

class SectionDetailsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var actionbar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_details)

        //Define the toolbar for the activity
        this.toolbar = toolBar
        setSupportActionBar(toolbar)

        //Set properties for the ActionBar in the activity
        this.actionbar = supportActionBar!!
        this.actionbar.setDisplayHomeAsUpEnabled(true)

        //Set the title of the activity
        this.actionbar.title = getString(R.string.secdet_title)

        //Get data from previous activity
        val bundle: Bundle = intent.getBundleExtra("bundle")!!

        val session: WASession = bundle.getParcelable("session")!!
        val result: SearchResult = bundle.getParcelable("result")!!
        val cookies: HashMap<String, String> = intent.extras!!.getSerializable("cookies") as HashMap<String, String>

        //Create an error handler for the coroutine that will be executed to get the section details
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@SectionDetailsActivity, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(this@SectionDetailsActivity, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the section details
        CoroutineScope(errorHandler).launch {
            //Get the section details
            val details: SectionDetails = session.getSectionDetails(cookies, result)

            //Update the fields in the UI, and make them visible
            withContext(Dispatchers.Main) {
                textView_sectionDetails_title.text = details.title
                textView_sectionDetails_description.text = details.description
                textView_sectionDetails_offerings.text = details.offerings
                textView_sectionDetails_restrictions.text = details.restrictions
                textView_sectionDetails_prereqs.text = details.prereqs
                textView_sectionDetails_departments.text = details.departments
                textView_sectionDetails_startDate.text = details.startDate
                textView_sectionDetails_endDate.text = details.endDate
                textView_sectionDetails_facultyName.text = details.facultyName
                textView_sectionDetails_facultyEmail.text = details.facultyEmail
                textView_sectionDetails_facultyPhone.text = details.facultyPhone
                textView_sectionDetails_facultyExtension.text = details.facultyExtension

                //If a field is not available, or not applicable, hide it
                //For contact info fields (email and phone), enable opening email links if tapped
                if (textView_sectionDetails_offerings.text == "") {
                    sectionDetails_offerings_container.visibility = View.GONE
                }
                if (textView_sectionDetails_restrictions.text == "") {
                    sectionDetails_restrictions_container.visibility = View.GONE
                }
                if (textView_sectionDetails_prereqs.text == "") {
                    sectionDetails_prereqs_container.visibility = View.GONE
                }
                if (textView_sectionDetails_facultyEmail.text == "") {
                    sectionDetails_email_container.visibility = View.GONE
                } else {
                    //Create a SpannableString and ClickableSpan to enable the email to be clicked like a link, and for an action to happen when that click occurs
                    val emailSpan: SpannableString = SpannableString(details.facultyEmail)
                    val emailClick: ClickableSpan = object: ClickableSpan() {
                        override fun onClick(view: View) {
                            //When an email is clicked on, create an intent that opens an email client
                            val intent: Intent = Intent(Intent.ACTION_VIEW).apply({
                                data = Uri.parse("mailto:?to=" + Uri.encode(details.facultyEmail))
                            })

                            //Start a new activity, allowing the user to send an email using any of the clients they've installed
                            try {
                                startActivity(Intent.createChooser(intent, getString(R.string.secdet_send_email_prompt)))
                            } catch(e: Exception) {
                                Toast.makeText(this@SectionDetailsActivity, getString(R.string.secdet_email_unable), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    //Set the span of the SpannableString to the ClickableSpan which triggers the email client chooser
                    emailSpan.setSpan(emailClick, 0, details.facultyEmail.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    //Set the text of the email TextView to the SpannableString
                    textView_sectionDetails_facultyEmail.text = emailSpan
                    //Enable links to work for the email TextView
                    textView_sectionDetails_facultyEmail.movementMethod = LinkMovementMethod.getInstance()
                }
                if (textView_sectionDetails_facultyPhone.text == "") {
                    sectionDetails_phone_container.visibility = View.GONE
                } else {
                    //Create a SpannableString and ClickableSpan to enable the phone number to be clicked like a link, and for an action to happen when that click occurs
                    val phoneSpan: SpannableString = SpannableString(details.facultyPhone)
                    val phoneClick: ClickableSpan = object: ClickableSpan() {
                        override fun onClick(view: View) {
                            //When a phone number is clicked on, create an intent that opens the dialer
                            val intent: Intent = Intent(Intent.ACTION_DIAL).apply {
                                //Remove all characters except the numbers from the phone number
                                data = Uri.parse("tel:" + Uri.encode(details.facultyPhone))
                            }

                            //Start a new activity, allowing the user to call the number using their dialer
                            try {
                                startActivity(intent)
                            } catch(e: Exception) {
                                Toast.makeText(this@SectionDetailsActivity, getString(R.string.secdet_phone_unable), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    //Set the span of the SpannableString to the ClickableSpan which triggers the dialer opening
                    phoneSpan.setSpan(phoneClick, 0, details.facultyPhone.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    //Set the text of the phone TextView to the SpannableString
                    textView_sectionDetails_facultyPhone.text = phoneSpan
                    //Enable links to work for the phone TextView
                    textView_sectionDetails_facultyPhone.movementMethod = LinkMovementMethod.getInstance()
                }
                if (textView_sectionDetails_facultyExtension.text == "") {
                    sectionDetails_extension_container.visibility = View.GONE
                }

                scrollView_sectionDetails_content.visibility = View.VISIBLE
            }
        }
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