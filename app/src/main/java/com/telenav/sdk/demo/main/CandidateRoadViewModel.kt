package com.telenav.sdk.demo.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.sdk.demo.util.SingleLiveEvent
import com.telenav.sdk.drivesession.model.CandidateRoadInfo

class CandidateRoadViewModel : ViewModel(){
    val candidateRoads = MutableLiveData<List<CandidateRoadInfo>>()
    val selectedRoad = SingleLiveEvent<CandidateRoadInfo>()
}