package io.github.ryangwsimmons.wamobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar

class SearchSectionsFragment(private var session: WASession, private var actionBar: ActionBar) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Set the title of the action bar
        actionBar.title = "Search for Sections"

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_sections, container, false)
    }
}