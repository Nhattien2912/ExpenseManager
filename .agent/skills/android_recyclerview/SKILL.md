---
name: Android RecyclerView
description: Hướng dẫn sử dụng RecyclerView với ListAdapter, DiffUtil, và các tính năng như swipe-to-delete.
---

# Kỹ năng: RecyclerView trong Android

## Khi nào sử dụng
- Hiển thị danh sách dữ liệu
- Cần scroll hiệu quả với nhiều items
- Implement swipe-to-delete, drag-and-drop

## Cấu trúc Adapter với ListAdapter (Recommended)

```kotlin
class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Boolean
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(getItem(position))
                } else false
            }
        }

        fun bind(transaction: Transaction) {
            binding.apply {
                tvDescription.text = transaction.description
                tvAmount.text = formatCurrency(transaction.amount)
                tvDate.text = formatDate(transaction.date)
                
                // Đổi màu theo income/expense
                val color = if (transaction.isExpense) 
                    R.color.expense_red else R.color.income_green
                tvAmount.setTextColor(ContextCompat.getColor(root.context, color))
            }
        }
    }
}
```

## DiffUtil Callback

```kotlin
class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}
```

## Setup trong Fragment/Activity

```kotlin
class TransactionListFragment : Fragment() {

    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                navigateToDetail(transaction.id)
            },
            onItemLongClick = { transaction ->
                showDeleteDialog(transaction)
                true
            }
        )

        binding.recyclerView.apply {
            this.adapter = this@TransactionListFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            
            // Thêm divider
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun observeData() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            
            // Show empty state
            binding.emptyView.isVisible = transactions.isEmpty()
        }
    }
}
```

## Swipe-to-Delete với ItemTouchHelper

```kotlin
private fun setupSwipeToDelete() {
    val swipeHandler = object : ItemTouchHelper.SimpleCallback(
        0,  // Drag directions (0 = không cho drag)
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT  // Swipe directions
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false  // Không cần drag-and-drop

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val item = adapter.currentList[position]
            
            // Xóa item
            viewModel.deleteTransaction(item)
            
            // Show undo snackbar
            Snackbar.make(binding.root, "Đã xóa giao dịch", Snackbar.LENGTH_LONG)
                .setAction("Hoàn tác") {
                    viewModel.insertTransaction(item)
                }
                .show()
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            // Vẽ background đỏ và icon delete khi swipe
            val itemView = viewHolder.itemView
            val background = ColorDrawable(Color.RED)
            val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
            
            val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + icon.intrinsicHeight

            when {
                dX > 0 -> { // Swipe right
                    background.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                    icon.setBounds(
                        itemView.left + iconMargin,
                        iconTop,
                        itemView.left + iconMargin + icon.intrinsicWidth,
                        iconBottom
                    )
                }
                dX < 0 -> { // Swipe left
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    icon.setBounds(
                        itemView.right - iconMargin - icon.intrinsicWidth,
                        iconTop,
                        itemView.right - iconMargin,
                        iconBottom
                    )
                }
                else -> {
                    background.setBounds(0, 0, 0, 0)
                }
            }
            
            background.draw(c)
            icon.draw(c)
            
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
}
```

## Multiple View Types

```kotlin
class MultiTypeAdapter : ListAdapter<ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.TransactionItem -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(...)
            TYPE_ITEM -> ItemViewHolder(...)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.TransactionItem -> (holder as ItemViewHolder).bind(item)
        }
    }
}

sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class TransactionItem(val transaction: Transaction) : ListItem()
}
```

## Best Practices

1. **Sử dụng ListAdapter** với DiffUtil thay vì RecyclerView.Adapter
2. **ViewBinding** trong ViewHolder
3. **Avoid expensive operations** trong onBindViewHolder
4. **setHasFixedSize(true)** nếu item size cố định
5. **RecyclerView.NO_POSITION check** khi handle clicks
6. **Sử dụng currentList** thay vì custom list variable
