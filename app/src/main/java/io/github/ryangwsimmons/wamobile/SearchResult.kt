package io.github.ryangwsimmons.wamobile

import android.text.Spanned

data class SearchResult(val term: String, val status: String, val title: String, val location: String, val meetings: ArrayList<String>, val faculty: String, val available: String, val credits: String, val academicLevel: String)