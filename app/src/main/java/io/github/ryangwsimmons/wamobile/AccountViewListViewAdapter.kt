package io.github.ryangwsimmons.wamobile

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import java.util.*

class AccountViewListViewAdapter(private val categories: ArrayList<AccountViewCategory>, private val context: Context): BaseExpandableListAdapter() {

    override fun getGroupCount() = this.categories.size

    override fun getChildrenCount(groupPosition: Int): Int = this.categories[groupPosition].transactions.size

    override fun getChildTypeCount(): Int = 2

    override fun getChildType(groupPosition: Int, childPosition: Int): Int {
        val child = getChild(groupPosition, childPosition)
        return if (child.chargeDetails.size == 0) {
            0
        } else {
            1
        }
    }

    override fun getGroup(groupPosition: Int): AccountViewCategory = this.categories[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): AccountViewTransaction = this.categories[groupPosition].transactions[childPosition]

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    @SuppressLint("InflateParams")
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        // Create main groups
        val groupConvertView: View = if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            layoutInflater.inflate(R.layout.account_view_list_group, null)
        } else {
            convertView
        }

        // Get the name and amount for the group
        val groupName: TextView = groupConvertView.findViewById(R.id.groupName)
        val groupAmount: TextView = groupConvertView.findViewById(R.id.groupAmount)

        groupName.text = getGroup(groupPosition).name

        val amountFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        amountFormat.currency = Currency.getInstance("CAD")
        groupAmount.text = amountFormat.format(getGroup(groupPosition).amount)

        return groupConvertView
    }

    // This function is based on code from the Three Level Expandable List View by Talha Hasan Zia,
    // available at https://github.com/talhahasanzia/Three-Level-Expandable-Listview.
    // Licensed under the Apache License Version 2.0: https://www.apache.org/licenses/LICENSE-2.0.html.
    // It is has been modified from the original source code.
    @SuppressLint("InflateParams")
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        // Create children (items and subgroups)
        val child = getChild(groupPosition, childPosition)

        var itemConvertView = if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val subGroupELV = SubGroupExpandableListView(this.context)

            when(getChildType(groupPosition, childPosition)) {
                0 -> layoutInflater.inflate(R.layout.account_view_list_item, null)
                1 -> subGroupELV
                else -> throw Exception("Invalid data format, group $groupPosition, child $childPosition")
            }
        } else {
            convertView
        }

        if (getChildType(groupPosition, childPosition) == 1) { // If the child is a subgroup
            // Create the subgroup expandable list view and adapter
            itemConvertView = itemConvertView as SubGroupExpandableListView

            itemConvertView.setAdapter(SubGroupListAdapter(this.context, child))
        } else { // if the child is an item

            // Define the item name and amount
            val itemName: TextView = itemConvertView.findViewById(R.id.itemName)
            itemName.text = child.name

            val itemAmount: TextView = itemConvertView.findViewById(R.id.itemAmount)
            val amountFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            amountFormat.currency = Currency.getInstance("CAD")
            itemAmount.text = amountFormat.format(child.amount)
        }

        return itemConvertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
}