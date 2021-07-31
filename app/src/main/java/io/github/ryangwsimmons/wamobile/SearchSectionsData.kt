package io.github.ryangwsimmons.wamobile

data class SearchSectionsData(
    val terms: ArrayList<DropdownOption> = arrayListOf<DropdownOption>(DropdownOption("Select Term", "")),
    val subjects: ArrayList<DropdownOption> = arrayListOf<DropdownOption>(DropdownOption("Subject", "")),
    val academicLevels: ArrayList<DropdownOption> = arrayListOf<DropdownOption>(DropdownOption("Select Academic Level", "")),
    val meetingTimes: ArrayList<DropdownOption> = arrayListOf<DropdownOption>(DropdownOption("Select Time Of Day", "")),
    val locations: ArrayList<DropdownOption> = arrayListOf<DropdownOption>(DropdownOption("Select Location", ""))
)