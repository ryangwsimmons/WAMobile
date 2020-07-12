package io.github.ryangwsimmons.wamobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
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

            //Get the username and password the user entered
            var username: String = editText_username.text.toString()
            var password: String = editText_password.text.toString()

            //Make sure that a username and password have been entered
            if (username == "" || password == "") {
                Toast.makeText(applicationContext, "Please enter a valid username and password.", Toast.LENGTH_SHORT).show()
                return
            }

            //Create a new WebAdvisor session
            val session: WASession = WASession(username, password)

            //Handle any errors that occur in the coroutine
            val errorHandler = CoroutineExceptionHandler { _, error ->
                CoroutineScope(Main).launch {
                    Toast.makeText(applicationContext, error.message ?: "A network error has occurred. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                    if (error.message != null) {
                        Toast.makeText(applicationContext, "An error has occurred. Please check your internet connection try again.", Toast.LENGTH_LONG).show()
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
                        putExtra("session", session)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    //Start the activity
                    startActivity(intent)
                }
            }
        })
    }
}