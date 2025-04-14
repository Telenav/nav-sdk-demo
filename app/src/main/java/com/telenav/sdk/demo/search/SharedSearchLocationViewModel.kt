package com.telenav.sdk.demo.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.sdk.examples.SearchResultItemDao

class SharedSearchLocationViewModel : ViewModel() {
    var mutableSelectedLocation = MutableLiveData<SearchResultItemDao>()

}