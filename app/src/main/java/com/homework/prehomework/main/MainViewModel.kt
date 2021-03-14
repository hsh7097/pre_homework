package com.homework.prehomework.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.homework.prehomework.base.BaseViewModel
import com.homework.prehomework.localRoom.RecentlyRoomDataBase
import com.homework.prehomework.localRoom.RecentlySearchWord
import com.homework.prehomework.localRoom.RecentlySearchWordDao
import com.homework.prehomework.main.adapter.MainContentAdapter.SortType
import com.homework.prehomework.main.repository.MainRepository
import com.homework.prehomework.main.repository.MainRepositoryImpl
import com.homework.prehomework.network.model.responseModel.RpSearchResult
import com.homework.prehomework.utils.SingleLiveData
import com.homework.prehomework.utils.extension.logError
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers

class MainViewModel(
    private val mainRepository: MainRepository =
        MainRepositoryImpl(),
    private val recentlySearchDao: RecentlySearchWordDao =
        RecentlyRoomDataBase.getInstance().recentlyDao()
) : BaseViewModel() {

    companion object {
        private const val PAGING_DEFAULT = 1
    }

    enum class SearchType(val value : String) {
        ALL("All"),
        BLOG("Blog"),
        CAFE("Cafe")
    }

    //상품 리스트
    private val _contentListLiveData = MutableLiveData<ArrayList<RpSearchResult.Document>>()
    val contentListLiveData: LiveData<ArrayList<RpSearchResult.Document>> get() = _contentListLiveData

    //상품 페이징 리스트
    private val _contentPageListLiveData = MutableLiveData<ArrayList<RpSearchResult.Document>>()
    val contentPageListLiveData: LiveData<ArrayList<RpSearchResult.Document>> get() = _contentPageListLiveData

    //최근 검색어 리스트
    private val _recentlySearchWordListLiveData = MutableLiveData<ArrayList<RecentlySearchWord>>()
    val recentlySearchWordListLiveData: LiveData<ArrayList<RecentlySearchWord>> get() = _recentlySearchWordListLiveData

    //정렬 타입
    private val _showSortTypeDialogLiveData = SingleLiveData<SortType>()
    val showSortTypeDialogLiveData: LiveData<SortType> get() = _showSortTypeDialogLiveData

    var searchWord: String? = null
    var searchPaging: Int = 1

    //검색 타입
    private var searchType: SearchType = SearchType.ALL

    fun callSearch(searchWord: String?) {
        if (searchWord.isNullOrEmpty()) return
        this.searchWord = searchWord
        recentlySearchDao.insert(RecentlySearchWord(word = searchWord))
        searchPaging = PAGING_DEFAULT
        when (searchType) {
            SearchType.BLOG -> getBlogSearch()
            SearchType.CAFE -> getCafeSearch()
            SearchType.ALL -> getAllSearch()
        }
    }

    fun callSearchPaging() {
        when (searchType) {
            SearchType.BLOG -> getBlogSearch()
            SearchType.CAFE -> getCafeSearch()
            SearchType.ALL -> getAllSearch()
        }
    }

    fun changeSearchType(searchType: SearchType) {
        if (this.searchType == searchType) return
        this.searchType = searchType
        callSearch(searchWord)
    }

    private fun getBlogSearch() {
        if (searchWord.isNullOrEmpty()) return
        mainRepository.getBlogSearch(
            query = searchWord!!,
            page = searchPaging
        ).flatMap { responseSearch ->
            responseSearch.documents.forEach {
                it.searchType = RpSearchResult.SearchType.BLOG
            }
            return@flatMap Single.just(responseSearch.documents)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableSingleObserver<List<RpSearchResult.Document>>() {
                override fun onSuccess(result: List<RpSearchResult.Document>) {
                    if (searchPaging == 1) {
                        _contentListLiveData.postValue(ArrayList(result))
                    } else {
                        _contentPageListLiveData.postValue(ArrayList(result))
                    }
                    searchPaging++
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }
            })
    }

    private fun getCafeSearch() {
        if (searchWord.isNullOrEmpty()) return
        mainRepository.getCafeSearch(
            query = searchWord!!,
            page = searchPaging
        ).flatMap { responseSearch ->
            responseSearch.documents.forEach {
                it.searchType = RpSearchResult.SearchType.CAFE
            }
            return@flatMap Single.just(responseSearch.documents)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableSingleObserver<List<RpSearchResult.Document>>() {
                override fun onSuccess(result: List<RpSearchResult.Document>) {
                    if (searchPaging == 1) {
                        _contentListLiveData.postValue(ArrayList(result))
                    } else {
                        _contentPageListLiveData.postValue(ArrayList(result))
                    }
                    searchPaging++
                }

                override fun onError(e: Throwable) {
                    logError("onError $e")
                    e.printStackTrace()
                }
            })
    }

    private fun getAllSearch() {
        if (searchWord.isNullOrEmpty()) return
        Single.zip(
            mainRepository.getBlogSearch(
                query = searchWord!!,
                page = searchPaging
            ),
            mainRepository.getCafeSearch(
                query = searchWord!!,
                page = searchPaging
            ),
            BiFunction { blogSearch: RpSearchResult, cafeSearch: RpSearchResult ->
                return@BiFunction ArrayList<RpSearchResult.Document>().apply {
                    blogSearch.documents.forEach {
                        it.searchType = RpSearchResult.SearchType.BLOG
                    }
                    cafeSearch.documents.forEach {
                        it.searchType = RpSearchResult.SearchType.CAFE
                    }
                    addAll(blogSearch.documents)
                    addAll(cafeSearch.documents)
                }
            }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableSingleObserver<List<RpSearchResult.Document>>() {
                override fun onSuccess(result: List<RpSearchResult.Document>) {
                    if (searchPaging == 1) {
                        _contentListLiveData.postValue(ArrayList(result))
                    } else {
                        _contentPageListLiveData.postValue(ArrayList(result))
                    }
                    searchPaging++
                }

                override fun onError(e: Throwable) {
                    logError("onError $e")
                    e.printStackTrace()
                }
            })

    }

    fun callShowRecentSearchLayout() {
        val searchWordList = ArrayList(recentlySearchDao.getRecentSearchedWords())
        if (searchWordList.isNotEmpty()) {
            _recentlySearchWordListLiveData.postValue(searchWordList)
        }
    }

    fun callShowSortDialog(sortType: SortType) {
        _showSortTypeDialogLiveData.postValue(sortType)
    }

}