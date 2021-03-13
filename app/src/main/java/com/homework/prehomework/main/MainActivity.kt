package com.homework.prehomework.main

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.homework.prehomework.R
import com.homework.prehomework.base.BaseActivity
import com.homework.prehomework.databinding.ActivityMainBinding
import com.homework.prehomework.localRoom.RecentlySearchWord
import com.homework.prehomework.main.adapter.MainContentAdapter
import com.homework.prehomework.utils.extension.dpToPixel
import com.homework.prehomework.utils.extension.getTextAndClear
import com.homework.prehomework.utils.extension.registerEnterKeyListener
import com.homework.prehomework.utils.extension.setClickAnimation
import com.homework.prehomework.widgets.itemDecoration.ItemVerticalDecorator
import org.jetbrains.anko.intentFor

class MainActivity : BaseActivity() {

    companion object {
        fun start(context: Context?) {
            context?.startActivity(
                context.intentFor<MainActivity>()
            )
        }
    }

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    private val mainContentAdapter by lazy {
        MainContentAdapter(mainViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBinding()
        setRecyclerView()
        setListener()
        observerLiveData()
    }

    override fun onBackPressed() {
        finishApplication()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.run {
            lifecycleOwner = this@MainActivity
            mainActivity = this@MainActivity
            viewModel = mainViewModel
        }
    }

    //RecyclerView 설정
    private fun setRecyclerView() {
        binding.contentRv.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                ItemVerticalDecorator(
                    topMargin = 8.dpToPixel(),
                    bottomMargin = 8.dpToPixel(),
                    startMargin = 0.dpToPixel(),
                    endMargin = 0.dpToPixel(),
                    firstTopMargin = 0.dpToPixel(),
                    lastBottomMargin = 8.dpToPixel()
                )
            )
            adapter = mainContentAdapter
        }

    }

    private fun observerLiveData() {
        with(mainViewModel) {
            recentlySearchWordListLiveData.observe(this@MainActivity, Observer {
                showRecentSearchLayout(it)
            })
        }
    }

    private fun callSearch() {
        val searchText = binding.searchEt.getTextAndClear()
        mainViewModel.callSearch(searchText)
    }

    private fun setListener() {
        binding.searchEt.run {
            registerEnterKeyListener {
                callSearch()
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    mainViewModel.callShowRecentSearchLayout()
                }
            }
        }
    }

    private fun showRecentSearchLayout(searchWordList: ArrayList<RecentlySearchWord>) {
        binding.recentSearchBackLayout.visibility = View.VISIBLE

    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.recentSearchBackLayout -> {
                binding.recentSearchBackLayout.visibility = View.GONE
            }
        }
    }

    fun onAnimClick(view: View) {
        view.setClickAnimation {
            when (view.id) {
                R.id.searchIv -> {
                    callSearch()
                }
            }
        }
    }

}