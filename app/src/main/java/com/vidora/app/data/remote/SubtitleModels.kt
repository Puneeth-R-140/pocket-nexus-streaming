package com.vidora.app.data.remote

import com.google.gson.annotations.SerializedName

data class SubtitleItem(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("format") val format: String,
    @SerializedName("display") val display: String,
    @SerializedName("language") val language: String,
    @SerializedName("media") val mediaTitle: String? = null,
    @SerializedName("isHearingImpaired") val isHearingImpaired: Boolean = false,
    @SerializedName("source") val source: String? = null,
    @SerializedName("release") val release: String? = null
)

typealias SubtitleResponse = List<SubtitleItem>
