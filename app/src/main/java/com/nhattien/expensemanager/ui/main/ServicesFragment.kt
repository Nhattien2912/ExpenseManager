package com.nhattien.expensemanager.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.nhattien.expensemanager.R

class ServicesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_services, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners(view)
        animateCards(view)
    }

    private fun setupClickListeners(view: View) {
        // Quản lý tài chính
        view.findViewById<View>(R.id.cardDebt).setOnClickListener {
            (activity as? MainActivity)?.loadFragment(
                com.nhattien.expensemanager.ui.debt.DebtFragment()
            )
        }

        view.findViewById<View>(R.id.cardSavings).setOnClickListener {
            (activity as? MainActivity)?.loadFragment(
                com.nhattien.expensemanager.ui.saving.SavingsFragment()
            )
        }

        view.findViewById<View>(R.id.cardBudget).setOnClickListener {
            (activity as? MainActivity)?.loadFragment(
                com.nhattien.expensemanager.ui.budget.BudgetFragment()
            )
        }

        view.findViewById<View>(R.id.cardPlanned).setOnClickListener {
            startActivity(Intent(requireContext(),
                com.nhattien.expensemanager.ui.planned.PlannedExpenseActivity::class.java))
        }

        view.findViewById<View>(R.id.cardWallet).setOnClickListener {
            startActivity(Intent(requireContext(),
                com.nhattien.expensemanager.ui.wallet.ManageWalletsActivity::class.java))
        }

        view.findViewById<View>(R.id.cardChart).setOnClickListener {
            // Charts are on the main screen Report tab
            val mainFragment = MainFragment.newInstance(true)
            (activity as? MainActivity)?.loadFragment(mainFragment)
        }

        // Công cụ
        view.findViewById<View>(R.id.cardCategory).setOnClickListener {
            startActivity(Intent(requireContext(),
                com.nhattien.expensemanager.ui.category.CategoryManagerActivity::class.java))
        }

        view.findViewById<View>(R.id.cardTags).setOnClickListener {
            startActivity(Intent(requireContext(),
                com.nhattien.expensemanager.ui.tag.ManageTagsActivity::class.java))
        }

        view.findViewById<View>(R.id.cardNotification).setOnClickListener {
            startActivity(Intent(requireContext(),
                com.nhattien.expensemanager.ui.notification.NotificationActivity::class.java))
        }

        view.findViewById<View>(R.id.cardSearch).setOnClickListener {
            startActivity(Intent(requireContext(),
                com.nhattien.expensemanager.ui.search.SearchActivity::class.java))
        }
    }

    private fun animateCards(view: View) {
        val cardIds = listOf(
            R.id.cardDebt, R.id.cardSavings, R.id.cardBudget,
            R.id.cardPlanned, R.id.cardWallet, R.id.cardChart,
            R.id.cardCategory, R.id.cardTags, R.id.cardNotification,
            R.id.cardSearch
        )

        cardIds.forEachIndexed { index, cardId ->
            val card = view.findViewById<View>(cardId)
            card.alpha = 0f
            card.translationY = 60f
            card.scaleX = 0.92f
            card.scaleY = 0.92f

            card.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay((index * 60).toLong())
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()
        }
    }
}
