package ca.ryangwsimmons.wamobile

import android.text.SpannedString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

class SearchResultsAdapter(private var results: List<SearchResult>, private val listener: OnSectionClickListener): RecyclerView.Adapter<SearchResultsAdapter.SectionHolder>() {

    //Create a view holder to hold the data for each section
    inner class SectionHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val title: TextView = itemView.findViewById(R.id.textView_section_courseTitle)
        val status: TextView = itemView.findViewById(R.id.textView_section_status)
        val location: TextView = itemView.findViewById(R.id.textView_section_location)
        val term: TextView = itemView.findViewById(R.id.textView_section_term)
        val faculty: TextView = itemView.findViewById(R.id.textView_section_faculty)
        val available: TextView = itemView.findViewById(R.id.textView_section_available)
        val credits: TextView = itemView.findViewById(R.id.textView_section_credits)
        val academicLevel: TextView = itemView.findViewById(R.id.textView_section_academicLevel)
        val meetings: TextView = itemView.findViewById(R.id.textView_section_meetings)

        //Set the listener function for the section being clicked to the listener function in this class
        init {
            itemView.setOnClickListener(this)
        }

        //On a section being clicked, trigger the click event in the listener
        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onSectionClick(position)
            }
        }
    }

    //Create an interface for responding to sections being clicked
    interface OnSectionClickListener {
        fun onSectionClick(position: Int)
    }

    //Write the function that creates ViewHolders to put the sections in
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycleritem_section, parent, false)

        return SectionHolder(itemView)
    }

    //Write the function that binds ViewHolders to data
    override fun onBindViewHolder(holder: SectionHolder, position: Int) {
        val currentResult = this.results[position]

        holder.title.text = SpannedString(currentResult.title.replaceFirst(") ", ")\n"))
        holder.status.text = currentResult.status
        holder.location.text = currentResult.location
        holder.term.text = currentResult.term
        holder.faculty.text = currentResult.faculty
        holder.available.text = currentResult.available
        holder.credits.text = currentResult.credits
        holder.academicLevel.text = currentResult.academicLevel
        val finalMeetingsString: StringBuilder = StringBuilder()
        currentResult.meetings.forEach {meeting ->
            finalMeetingsString.append(meeting)
        }
        holder.meetings.text = SpannedString(finalMeetingsString.toString())

        //If the current section is closed (has no available spots), set the background colour of the card to red.
        if (currentResult.status == "Closed") {
            holder.itemView.findViewById<RelativeLayout>(R.id.relativeLayout_sectionContainer).setBackgroundColor(ContextCompat.getColor(holder.itemView.findViewById<RelativeLayout>(R.id.relativeLayout_sectionContainer).context, R.color.colorEmptySection))
            holder.itemView.findViewById<View>(R.id.view_verticalDivider).setBackgroundColor(holder.itemView.findViewById<TextView>(R.id.textView_section_academicLevelLabel).textColors.defaultColor)
            holder.itemView.findViewById<View>(R.id.view_verticalDivider2).setBackgroundColor(holder.itemView.findViewById<TextView>(R.id.textView_section_academicLevelLabel).textColors.defaultColor)
        } else {
            holder.itemView.findViewById<RelativeLayout>(R.id.relativeLayout_sectionContainer).setBackgroundColor(ContextCompat.getColor(holder.itemView.findViewById<RelativeLayout>(R.id.relativeLayout_sectionContainer).context, R.color.design_default_color_background))
            holder.itemView.findViewById<View>(R.id.view_verticalDivider).setBackgroundColor(ContextCompat.getColor(holder.itemView.findViewById<View>(R.id.view_verticalDivider).context, R.color.colorShaded))
            holder.itemView.findViewById<View>(R.id.view_verticalDivider2).setBackgroundColor(ContextCompat.getColor(holder.itemView.findViewById<View>(R.id.view_verticalDivider2).context, R.color.colorShaded))
        }
    }

    override fun getItemCount() = this.results.size

    //Replace the list of items for the adapter
    fun setItems(newResults: List<SearchResult>) {
        this.results = newResults
    }
}