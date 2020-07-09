package io.github.ryangwsimmons.wamobile

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_user_menu.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Get the session object
        val session: WASession = getIntent().getSerializableExtra("session") as WASession

        //Get the user's name
        try {
            //Launch a new coroutine
            CoroutineScope(IO).launch {
                //Connect to WebAdvisor and get the user's name
                val name = session.getName()

                //Set the text of the "hello" textView
                withContext(Main) {
                    textView_hello.text = "Hello, " + name
                }
            }
        } catch (e: Exception) {
            textView_hello.setTextColor(Color.RED)
            textView_hello.text = e.message
        }

        setContentView(R.layout.activity_user_menu)
    }
}