package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class NewsFragment(private var session: WASession, var actionBar: ActionBar) : Fragment() {

    lateinit var newsItems: List<NewsItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Change the title of the action bar to "News"
        actionBar.title = getString(R.string.news_title)

        //Get the items in the fragment
        val listItems: View = inflater.inflate(R.layout.fragment_news, container, false)

        //Get the recycler view from the fragment
        var recyclerViewNewsItems: RecyclerView = listItems.findViewById<RecyclerView>(R.id.recyclerView_news)

        //Set the adapter, layout manager, and other settings for the recycler view for the news items
        var adapter: NewsAdapter = NewsAdapter(ArrayList<NewsItem>(), activity!!)
        recyclerViewNewsItems.adapter = adapter
        recyclerViewNewsItems.layoutManager = LinearLayoutManager(activity!!.applicationContext)

        //Create an error handler for the coroutine that will be executed to get the news items
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(activity!!.applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(activity!!.applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the news items
        CoroutineScope(errorHandler).launch {
            //Get a list with the news items
            this@NewsFragment.newsItems = session.getNewsItems()

            withContext(Dispatchers.Main) {
                adapter.setItems(this@NewsFragment.newsItems)
                adapter.notifyItemInserted(this@NewsFragment.newsItems.size - 1)
            }
        }

        // Inflate the layout for this fragment
        return listItems
    }
}