package io.github.ryangwsimmons.wamobile

import android.graphics.Color
import android.text.SpannedString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycleritem_section.view.*
import kotlin.text.StringBuilder

class SearchResultsAdapter(private var results: List<SearchResult>): RecyclerView.Adapter<SearchResultsAdapter.SectionHolder>() {

    //Create a view holder to hold the data for each section
    inner class SectionHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.textView_section_courseTitle
        val status: TextView = itemView.textView_section_status
        val location: TextView = itemView.textView_section_location
        val term: TextView = itemView.textView_section_term
        val faculty: TextView = itemView.textView_section_faculty
        val available: TextView = itemView.textView_section_available
        val credits: TextView = itemView.textView_section_credits
        val academicLevel: TextView = itemView.textView_section_academicLevel
        val meetings: TextView = itemView.textView_section_meetings
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
        var finalMeetingsString: StringBuilder = StringBuilder()
        currentResult.meetings.forEach {meeting ->
            finalMeetingsString.append(meeting)
        }
        holder.meetings.text = SpannedString(finalMeetingsString.toString())

        //If the current section is closed (has no available spots), set the background colour of the card to red.
        if (currentResult.status == "Closed") {
            holder.itemView.relativeLayout_sectionContainer.setBackgroundColor(Color.parseColor("#f7aaaa"))
            holder.itemView.view_verticalDivider.setBackgroundColor(holder.itemView.textView_section_academicLevelLabel.textColors.defaultColor)
            holder.itemView.view_verticalDivider2.setBackgroundColor(holder.itemView.textView_section_academicLevelLabel.textColors.defaultColor)
        } else {
            holder.itemView.relativeLayout_sectionContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.relativeLayout_sectionContainer.context, R.color.design_default_color_background))
            holder.itemView.view_verticalDivider.setBackgroundColor(Color.parseColor("#e5e5e5"))
            holder.itemView.view_verticalDivider2.setBackgroundColor(Color.parseColor("#e5e5e5"))
        }
    }

    override fun getItemCount() = this.results.size

    //Replace the list of items for the adapter
    fun setItems(newResults: List<SearchResult>) {
        this.results = newResults
    }
}