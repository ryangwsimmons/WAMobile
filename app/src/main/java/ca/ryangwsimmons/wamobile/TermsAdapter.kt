package ca.ryangwsimmons.wamobile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TermsAdapter(private var terms: List<Term>, private val listener: OnTermClickListener, private val context: Context): RecyclerView.Adapter<TermsAdapter.TermViewHolder>() {

    private var inflater: LayoutInflater = LayoutInflater.from(this.context)

    init {
        //Create an inflater using the context passed in from the fragment
    }

    //Create a view holder to hold the data for each term
    inner class TermViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val longName: TextView = itemView.findViewById(R.id.textView_term_longName)
        val shortName: TextView = itemView.findViewById(R.id.textView_term_shortName)
        val startDate: TextView = itemView.findViewById(R.id.textView_term_startDate)
        val endDate: TextView = itemView.findViewById(R.id.textView_term_endDate)

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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewHolder {
        //Inflate view holders using the activity context passed in
        val itemView = this.inflater.inflate(R.layout.recycleritem_term, parent, false)

        return TermViewHolder(itemView)
    }

    //Assign the values in the view holder to the values from the terms list
    override fun onBindViewHolder(holder: TermViewHolder, position: Int) {
        val currentTerm = this.terms[position]

        holder.longName.text = currentTerm.longName
        holder.shortName.text = currentTerm.shortName
        holder.startDate.text = holder.startDate.context.getString(R.string.termSelect_startDate) + currentTerm.startDate
        holder.endDate.text = holder.endDate.context.getString(R.string.termSelect_endDate) + currentTerm.endDate
    }

    //Set the size of the recycler view to the number of terms in the list
    override fun getItemCount() = this.terms.size

    //Update the list of items
    fun setItems(newTerms: List<Term>) {
        this.terms = newTerms
    }
}