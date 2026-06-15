package com.telenav.sdk.demo.search

import com.telenav.sdk.entity.model.prediction.EntityWordPredictionResponse
import com.telenav.searchservice.api.SearchBody
import com.telenav.searchservice.api.SearchResponse

fun SearchResponse.wordPredictionOrNull(): EntityWordPredictionResponse? =
    (body as? SearchBody.Sdk)?.value as? EntityWordPredictionResponse
