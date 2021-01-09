package io.github.ryangwsimmons.wamobile

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Show the initial dialog that appears when the app is started for the first time
        val prefs: SharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val firstStart: Boolean = prefs.getBoolean("firstStart", true)
        if (firstStart) {
            showInitialDialog()
        }

        //Set the action for when the "Login" button is tapped
        button_login.setOnClickListener(fun (v: View) {

            //Get the username and password the user entered
            val username: String = editText_username.text.toString()
            val password: String = editText_password.text.toString()

            //Make sure that a username and password have been entered
            if (username == "" || password == "") {
                Toast.makeText(applicationContext, getString(R.string.invalid_creds_message), Toast.LENGTH_SHORT).show()
                return
            }

            //Create a new WebAdvisor session
            val session: WASession = WASession(username, password, HashMap())

            //Handle any errors that occur in the coroutine
            val errorHandler = CoroutineExceptionHandler { _, error ->
                CoroutineScope(Main).launch {
                    Toast.makeText(applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                    if (error.message != null) {
                        Toast.makeText(applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                    }
                }
            }

            CoroutineScope(errorHandler).launch {
                //Initiate the connection to WebAdvisor
                session.initConnection()

                //Change the activity
                withContext(Main) {
                    //Create an intent to change activity
                    val intent = Intent(this@MainActivity, BaseActivity::class.java).apply {
                        //Set up the parcel for passing data to the activity
                        val bundle: Bundle = Bundle()
                        bundle.putParcelable("session", session)
                        putExtra("bundle", bundle)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    //Start the activity
                    startActivity(intent)
                }
            }
        })
    }

    private fun showInitialDialog() {
        //Show the dialog
        var dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.init_warning_title))
            .setMessage(Html.fromHtml(getString(R.string.init_warning_body), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL))
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()

        dialog.findViewById<TextView>(android.R.id.message)!!.movementMethod = LinkMovementMethod.getInstance()

        //Update the shared preferences so that the dialog doesn't appear again
        var prefs: SharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        var editor: SharedPreferences.Editor = prefs.edit()
        editor.putBoolean("firstStart", false)
        editor.apply()
    }
}