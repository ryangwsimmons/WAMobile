package io.github.ryangwsimmons.wamobile

data class SearchResultsData(val cookies: MutableMap<String, String>, val reqVerToken: String, val resultsJson: String)