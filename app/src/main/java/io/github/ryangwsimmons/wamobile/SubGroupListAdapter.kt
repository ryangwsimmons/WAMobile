package io.github.ryangwsimmons.wamobile

import android.content.Context
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class SubGroupListAdapter(private val parentContext: Context, private val transactions: JSONArray): BaseExpandableListAdapter() {
    override fun getGroupCount(): Int = this.transactions.length()

    override fun getChildrenCount(groupPosition: Int): Int = this.transactions
        .getJSONObject(groupPosition)
        .getJSONArray("ChargeDetails").length()

    override fun getGroup(groupPosition: Int): Any = this.transactions.getJSONObject(groupPosition)

    override fun getChild(groupPosition: Int, childPosition: Int): Any = this.transactions
        .getJSONObject(groupPosition)
        .getJSONArray("ChargeDetails")
        .getJSONObject(childPosition)

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        // Create main groups
        val groupConvertView: View = if (convertView == null) {
            val layoutInflater = this.parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            layoutInflater.inflate(R.layout.account_view_list_subgroup, null)
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

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val layoutInflater = this.parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemConvertView = layoutInflater.inflate(R.layout.account_view_list_item, null)

        val child = getChild(groupPosition, childPosition) as JSONObject

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

        return itemConvertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false

}