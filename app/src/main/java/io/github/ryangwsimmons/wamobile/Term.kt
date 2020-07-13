package io.github.ryangwsimmons.wamobile

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class Term(val shortName: String, val longName: String, val startDate: String, val endDate: String): Parcelable