package ca.ryangwsimmons.wamobile

import android.content.Context
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import java.util.*

class SubGroupListAdapter(private val parentContext: Context, private val transaction: AccountViewTransaction): BaseExpandableListAdapter() {
    override fun getGroupCount(): Int = 1

    override fun getChildrenCount(groupPosition: Int): Int = this.transaction.chargeDetails.size

    override fun getGroup(groupPosition: Int): AccountViewTransaction = this.transaction

    override fun getChild(groupPosition: Int, childPosition: Int): AccountViewChargeDetails = this.transaction.chargeDetails[childPosition]

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

        // Define the group name and amount
        val groupName: TextView = groupConvertView.findViewById(R.id.groupName)
        val groupAmount: TextView = groupConvertView.findViewById(R.id.groupAmount)

        groupName.text = getGroup(groupPosition).name

        val amountFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        amountFormat.currency = Currency.getInstance("CAD")
        groupAmount.text = amountFormat.format(getGroup(groupPosition).amount)

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

        // Get the data for the child
        val child = getChild(groupPosition, childPosition)

        // Define the child name and amount
        val itemName: TextView = itemConvertView.findViewById(R.id.itemName)
        itemName.text = child.name

        val itemAmount: TextView = itemConvertView.findViewById(R.id.itemAmount)
        val amountFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        amountFormat.currency = Currency.getInstance("CAD")
        itemAmount.text = amountFormat.format(child.amount)

        return itemConvertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false

}