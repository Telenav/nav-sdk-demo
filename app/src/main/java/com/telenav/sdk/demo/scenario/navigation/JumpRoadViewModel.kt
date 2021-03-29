package com.telenav.sdk.demo.scenario.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.sdk.demo.util.SingleLiveEvent
import com.telenav.sdk.drivesession.model.CandidateRoadInfo

class JumpRoadViewModel : ViewModel(){
    val candidateRoads = MutableLiveData<List<CandidateRoadInfo>>()
    val selectedRoad = SingleLiveEvent<CandidateRoadInfo>()
}