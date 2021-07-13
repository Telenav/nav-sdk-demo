package com.telenav.sdk.demo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.sdk.demo.SearchResultItemDao

class SharedSearchLocationViewModel : ViewModel() {
    val mutableSelectedLocation = MutableLiveData<SearchResultItemDao>()

}