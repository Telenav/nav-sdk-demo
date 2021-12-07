package com.telenav.sdk.demo.scenario.mapview

import androidx.lifecycle.ViewModel
import com.telenav.map.api.controllers.ShapesController

class MapViewShapeViewModel : ViewModel() {
    private val shapeList: MutableList<ShapesController.Id> = mutableListOf()
    fun add(id: ShapesController.Id) {
        shapeList.add(id)
    }

    fun remove(id: ShapesController.Id) {
        shapeList.add(id)
    }

    fun getAll(): List<ShapesController.Id> {
        return shapeList.toList()
    }
}