package io.github.ryangwsimmons.wamobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Set the action for when the "Login" button is tapped
        button_login.setOnClickListener(fun (v: View) {
            //Make the bad login text invisible
            textView_badLogin.visibility = View.INVISIBLE

            //Get the username and password the user entered
            var username: String = editText_username.text.toString()
            var password: String = editText_password.text.toString()

            //Make sure that a username and password have been entered
            if (username == "" || password == "") {
                textView_badLogin.text = "Please enter a valid username and password."
                textView_badLogin.visibility = View.VISIBLE

                return
            }

            //Create a new WebAdvisor session
            val session: WASession = WASession(username, password)

            try {
                CoroutineScope(IO).launch {
                    //Initiate the connection to WebAdvisor
                    if (session.initConnection() != "success") {
                        throw Exception("A network error has occurred. Please check your internet connection and try again.")
                    }

                    //Change the activity
                    withContext(Main) {
                        //Create an intent to change activity
                        val intent = Intent(this@MainActivity, UserMenuActivity::class.java).apply {
                            putExtra("session", session)
                        }

                        //Start the activity
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                textView_badLogin.text = e.message
                textView_badLogin.visibility = View.VISIBLE
            }
        })
    }
}