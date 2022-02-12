package com.charlye934.distancetrackerapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Result(
    var distance: String,
    var timer: String
): Parcelable