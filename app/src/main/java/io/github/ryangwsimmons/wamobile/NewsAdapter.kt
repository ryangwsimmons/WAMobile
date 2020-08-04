package io.github.ryangwsimmons.wamobile

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycleritem_news.view.*

class NewsAdapter(private var newsItems: List<NewsItem>, private val context: Context): RecyclerView.Adapter<NewsAdapter.NewsItemViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(this.context)

    init {
        //Create an inflater using the context passed in from the fragment
    }

    //Create a view holder to hold the data for each news item
    inner class NewsItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val heading1: TextView = itemView.textView_newsHeading1
        val heading2: TextView = itemView.textView_newsHeading2
        val body: TextView = itemView.textView_newsBody
    }

    //Initialize a new view holder, and associate it with the CardView for a news item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsItemViewHolder {
        //Inflate view holders using the context passed in
        val itemView = this.inflater.inflate(R.layout.recycleritem_news, parent, false)

        return NewsItemViewHolder(itemView)
    }

    //Assign the values in the view holder to the values from the news item list
    override fun onBindViewHolder(holder: NewsItemViewHolder, position: Int) {
        val currentItem = this.newsItems[position]

        holder.heading1.text = currentItem.heading1
        holder.heading2.text = currentItem.heading2
        holder.body.text = currentItem.body

        //Enable links in the news body
        holder.body.movementMethod = LinkMovementMethod.getInstance()
        holder.body.linksClickable = true
    }

    //Set the size of the recycler view to the number of items in the list
    override fun getItemCount() = this.newsItems.size

    //Update the list of items
    fun setItems(newNewsItems: List<NewsItem>) {
        this.newsItems = newNewsItems
    }
}