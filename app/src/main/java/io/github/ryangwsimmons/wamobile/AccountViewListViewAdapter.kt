package io.github.ryangwsimmons.wamobile

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class AccountViewListViewAdapter(private val transactions: JSONArray, private val context: Context): BaseExpandableListAdapter() {

    override fun getGroupCount() = this.transactions.length()

    override fun getChildrenCount(groupPosition: Int): Int = this.transactions
    .getJSONObject(groupPosition)
    .getJSONArray("Transactions").length()

    override fun getChildTypeCount(): Int = 2

    override fun getChildType(groupPosition: Int, childPosition: Int): Int {
        val child = getChild(groupPosition, childPosition) as JSONObject
        return if (!child.has("ChargeDetails")) {
            0
        } else {
            1
        }
    }

    override fun getGroup(groupPosition: Int): Any = this.transactions.getJSONObject(groupPosition)

    override fun getChild(groupPosition: Int, childPosition: Int): Any = this.transactions
        .getJSONObject(groupPosition)
        .getJSONArray("Transactions")
        .getJSONObject(childPosition)

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

        val groupName: TextView = groupConvertView.findViewById(R.id.groupName)
        val groupAmount: TextView = groupConvertView.findViewById(R.id.groupAmount)

        groupName.text = (getGroup(groupPosition) as JSONObject).getString("Description")

        val amountFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        amountFormat.currency = Currency.getInstance("CAD")
        groupAmount.text = amountFormat.format((getGroup(groupPosition) as JSONObject).getDouble("Amount"))

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
        val child = getChild(groupPosition, childPosition) as JSONObject

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

        if (getChildType(groupPosition, childPosition) == 1) {
            itemConvertView = itemConvertView as SubGroupExpandableListView

            itemConvertView.setAdapter(SubGroupListAdapter(this.context, JSONArray().put(child)))

            /*itemConvertView.setOnGroupExpandListener(object: ExpandableListView.OnGroupExpandListener {
                var previousGroup = -1

                override fun onGroupExpand(groupPosition: Int) {
                    if (groupPosition != previousGroup) {
                        itemConvertView.collapseGroup(previousGroup)
                    }
                    previousGroup = groupPosition
                }
            })*/
        } else {
            val itemName: TextView = itemConvertView.findViewById(R.id.itemName)
            itemName.text = when {
                child.has("Description") -> {
                    child.getString("Description")
                }
                child.has("Name") -> {
                    child.getString("Name")
                }
                child.has("PaymentMethodCode") -> {
                    child.getString("PaymentMethodCode")
                }
                child.has("TypeDescription") -> {
                    child.getString("TypeDescription")
                }
                else -> {
                    "Unknown Transaction Item $childPosition"
                }
            }

            val itemAmount: TextView = itemConvertView.findViewById(R.id.itemAmount)
            val amountFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            amountFormat.currency = Currency.getInstance("CAD")
            itemAmount.text = amountFormat.format(child.getDouble("Amount"))
        }

        return itemConvertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
}