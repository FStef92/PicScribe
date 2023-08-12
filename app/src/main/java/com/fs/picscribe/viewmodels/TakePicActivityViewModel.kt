package com.fs.picscribe.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel

class TakePicActivityViewModel : ViewModel() {


    fun sendMessage(msg : String) {
        Log.d("PicScribe", "sendMessage: $msg")}

}