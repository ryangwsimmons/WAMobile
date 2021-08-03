package ca.ryangwsimmons.wamobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GradeViewAdapter(private var grades: List<Grade>): RecyclerView.Adapter<GradeViewAdapter.GradeViewHolder>() {

    //Create a view holder to hold the data for each grade
    inner class GradeViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val courseSection: TextView = itemView.findViewById(R.id.textView_grades_courseSection)
        val courseTitle: TextView = itemView.findViewById(R.id.textView_grades_courseTitle)
        val finalGrade: TextView = itemView.findViewById(R.id.textView_grades_finalGrade)
        val credits: TextView = itemView.findViewById(R.id.textView_grades_Credits)
    }

    //Write the function that creates ViewHolders to put grades in
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycleritem_grade, parent, false)

        return GradeViewHolder(itemView)
    }

    //Write the function that binds ViewHolders to data
    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val currentGrade = this.grades[position]

        holder.courseSection.text = currentGrade.courseSection
        holder.courseTitle.text = currentGrade.courseTitle
        holder.finalGrade.text = holder.finalGrade.context.getString(R.string.finalGrade_label) + currentGrade.finalGrade
        holder.credits.text = holder.credits.context.getString(R.string.credits_label) + currentGrade.credits
    }

    override fun getItemCount() = this.grades.size

    //Replace the list of items for the adapter
    fun setItems(newGrades: List<Grade>) {
        this.grades = newGrades
    }
}