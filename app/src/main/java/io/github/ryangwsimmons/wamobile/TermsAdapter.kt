package io.github.ryangwsimmons.wamobile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycleritem_term.view.*

class TermsAdapter(private var terms: List<Term>, private val listener: OnTermClickListener, private val context: Context): RecyclerView.Adapter<TermsAdapter.TermViewwHolder>() {

    var inflater: LayoutInflater

    init {
        //Create an inflator using the context passed in from the fragment
        this.inflater = LayoutInflater.from(this.context)
    }

    //Create a view holder to hold the data for each term
    inner class TermViewwHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val longName: TextView = itemView.textView_term_longName
        val shortName: TextView = itemView.textView_term_shortName
        val startDate: TextView = itemView.textView_term_startDate
        val endDate: TextView = itemView.textView_term_endDate

        //Set the listener function for the term being clicked to the listener function in this class
        init {
            itemView.setOnClickListener(this)
        }

        //On a term being clicked, trigger the click event in the listener
        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onTermClick(position)
            }
        }
    }

    //Create an interface for responding to terms being clicked
    interface OnTermClickListener {
        fun onTermClick(position: Int)
    }

    //Create a new view holder, and associate it with the CardView for a term
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewwHolder {
        //Inflate view holders using the activity context passed in
        val itemView = this.inflater.inflate(R.layout.recycleritem_term, parent, false)

        return TermViewwHolder(itemView)
    }

    //Assign the values in the view holder to the values from the terms list
    override fun onBindViewHolder(holder: TermViewwHolder, position: Int) {
        val currentTerm = terms[position]

        holder.longName.text = currentTerm.longName
        holder.shortName.text = currentTerm.shortName
        holder.startDate.text = "Start Date: " + currentTerm.startDate
        holder.endDate.text = "End Date: " + currentTerm.endDate
    }

    //Set the size of the recycler view to the number of terms in the list
    override fun getItemCount() = terms.size

    //Update the list of items
    fun setItems(newTerms: List<Term>) {
        this.terms = newTerms
    }
}