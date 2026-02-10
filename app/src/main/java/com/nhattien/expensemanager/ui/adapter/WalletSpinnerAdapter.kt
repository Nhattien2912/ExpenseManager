package com.nhattien.expensemanager.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.WalletEntity

class WalletSpinnerAdapter(
    context: Context,
    private val wallets: List<WalletEntity>
) : ArrayAdapter<WalletEntity>(context, 0, wallets) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_wallet, parent, false)
        val wallet = getItem(position)

        if (wallet != null) {
            val txtIcon = view.findViewById<TextView>(R.id.txtStartIcon)
            val txtName = view.findViewById<TextView>(R.id.txtStartName)

            txtIcon.text = wallet.icon
            // txtIcon.background.setTint(wallet.color) // Optional layout polish
            txtName.text = wallet.name
        }
        return view
    }
}
