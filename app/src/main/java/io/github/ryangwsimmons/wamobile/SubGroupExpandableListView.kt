package io.github.ryangwsimmons.wamobile

import android.content.Context
import android.widget.ExpandableListView

// This class is based on code from the Three Level Expandable List View by Talha Hasan Zia,
// available at https://github.com/talhahasanzia/Three-Level-Expandable-Listview.
// Licensed under the Apache License Version 2.0: https://www.apache.org/licenses/LICENSE-2.0.html.
// It is has been modified from the original source code.
class SubGroupExpandableListView(context: Context): ExpandableListView(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 999999 is a size in pixels. ExpandableListView requires a maximum height in order to do measurement calculations.
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(999999, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

}