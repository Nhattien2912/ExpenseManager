---
name: Android Fragment
description: Hướng dẫn tạo và quản lý Fragment trong Android với Kotlin, bao gồm lifecycle, communication, và ViewPager.
---

# Kỹ năng: Tạo và Quản lý Android Fragment

## Khi nào sử dụng
- Tạo UI có thể tái sử dụng
- Xây dựng bottom navigation với nhiều tab
- Sử dụng ViewPager với các tab swipe được

## Cấu trúc Fragment chuẩn

```kotlin
class ExampleFragment : Fragment() {

    private var _binding: FragmentExampleBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExampleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExampleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.button.setOnClickListener {
            // Handle click
        }
    }

    private fun setupObservers() {
        viewModel.data.observe(viewLifecycleOwner) { data ->
            // Update UI
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Quan trọng: tránh memory leak
    }

    companion object {
        private const val ARG_PARAM = "param"
        
        fun newInstance(param: String): ExampleFragment {
            return ExampleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM, param)
                }
            }
        }
    }
}
```

## Checklist khi tạo Fragment mới

1. **Tạo file Kotlin**: `app/src/main/java/.../ui/[feature]/[Name]Fragment.kt`
2. **Tạo layout XML**: `app/src/main/res/layout/fragment_[name].xml`
3. **Tạo ViewModel** (nếu cần): `[Name]ViewModel.kt`
4. **Thêm vào Navigation Graph** hoặc FragmentManager

## Fragment Lifecycle

```
onAttach() → onCreate() → onCreateView() → onViewCreated() 
→ onStart() → onResume() → [RUNNING] 
→ onPause() → onStop() → onDestroyView() → onDestroy() → onDetach()
```

> **Lưu ý quan trọng**: 
> - `onDestroyView()` được gọi khi view bị destroy nhưng fragment vẫn còn
> - Luôn set `_binding = null` trong `onDestroyView()` để tránh memory leak
> - Sử dụng `viewLifecycleOwner` thay vì `this` khi observe LiveData

## Giao tiếp giữa Fragment và Activity

### Cách 1: Shared ViewModel (Recommended)
```kotlin
// Trong cả Fragment và Activity
private val sharedViewModel: SharedViewModel by activityViewModels()
```

### Cách 2: Interface Callback
```kotlin
interface OnItemSelectedListener {
    fun onItemSelected(item: Item)
}

class MyFragment : Fragment() {
    private var listener: OnItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnItemSelectedListener
    }

    private fun selectItem(item: Item) {
        listener?.onItemSelected(item)
    }
}
```

### Cách 3: Fragment Result API
```kotlin
// Fragment gửi
setFragmentResult("requestKey", bundleOf("data" to value))

// Fragment/Activity nhận
setFragmentResultListener("requestKey") { _, bundle ->
    val result = bundle.getString("data")
}
```

## Fragment với ViewPager2

```kotlin
class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    private val fragments = listOf(
        Tab1Fragment(),
        Tab2Fragment(),
        Tab3Fragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}

// Trong Fragment chứa ViewPager
binding.viewPager.adapter = ViewPagerAdapter(this)
TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
    tab.text = when (position) {
        0 -> "Tab 1"
        1 -> "Tab 2"
        else -> "Tab 3"
    }
}.attach()
```

## Best Practices

1. **Không giữ reference đến View sau onDestroyView**
2. **Sử dụng viewLifecycleOwner** cho UI-related observations
3. **Truyền data qua arguments**, không qua constructor
4. **Tránh reference đến Activity** trong Fragment
5. **Sử dụng Navigation Component** cho navigation phức tạp
