package com.isaka.munavigation.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.isaka.munavigation.adapter.CategoryListAdapter
import com.isaka.munavigation.databinding.ActivityCategoryBinding
import com.isaka.munavigation.model.Category

class CategoryActivity : BaseActivity(), CategoryListAdapter.OnItemClickListener {
    private val context = this
    private lateinit var binding: ActivityCategoryBinding
    private lateinit var adapter: CategoryListAdapter
    private lateinit var searchView: SearchView
    private var categoryList: MutableList<Category> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        searchView = binding.categorySearch

        // toolbar setup
        setupActionBar()

        // binding category list adapter
        adapter = CategoryListAdapter(mutableListOf(), context)
        populateCategoryListToUi()

        // category query filter
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterCategoryList(newText)
                }
                return true
            }
        })
    }

    override fun onItemClick(alias: String) {
        val category = getCategoryByAlias(alias)
        val intent = Intent(this, LocationActivity::class.java)
        intent.putExtra(EXTRA_ACTION_BAR_TITLE, category.name)
        startActivity(intent)
    }

    private fun populateCategoryListToUi() {
        categoryList = getCategoryList() as MutableList<Category>

        // set categories to adapter
        if (categoryList.isNotEmpty()) {
            binding.llCategoryListContainer.visibility = View.VISIBLE
            categoryList.sortBy { it.name }
            adapter = CategoryListAdapter(categoryList, context)
            binding.rvCategoryList.adapter = adapter
            binding.rvCategoryList.layoutManager = LinearLayoutManager(context)
            binding.rvCategoryList.setHasFixedSize(true)
        } else {
            binding.tvNoCategory.visibility = View.VISIBLE
        }
    }

    private fun filterCategoryList(text: String) {
        val filteredList = categoryList.filter { it.name.lowercase().contains(text.lowercase()) }
        adapter.filterList(filteredList)

        if (filteredList.isNotEmpty()) {
            binding.tvNoCategory.visibility = View.GONE
        } else {
            binding.tvNoCategory.visibility = View.VISIBLE
        }
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbCategory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbCategory.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_ACTION_BAR_TITLE = "extra_action_bar_title"
        const val TOOL_BAR_TITLE = "Category"
    }
}