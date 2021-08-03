package ca.ryangwsimmons.wamobile

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchResult(
    val term: String,
    val status: String,
    val title: String,
    val location: String,
    val meetings: ArrayList<String>,
    val faculty: String,
    val available: String,
    val credits: String,
    val academicLevel: String,
    val unparsedDescriptionString: String, // Named like this as while the value is labelled "CourseDescription" in the JSON, it also contains course specifications
    val sectionId: String
): Parcelable