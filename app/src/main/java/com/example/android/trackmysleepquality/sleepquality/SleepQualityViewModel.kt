package com.example.android.trackmysleepquality.sleepquality

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

class SleepTrackerViewModel(val database: SleepDatabaseDao,
                            application: Application): AndroidViewModel(application) {



    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }


    private var tonight= MutableLiveData<SleepNight?>()

    private val  nights= database.getAllNights()

    val nightsString= Transformations.map(nights){ nights->
        formatNights(nights, application.resources)
    }
    init {
        initializeToNight()
    }

    private fun initializeToNight() {
        viewModelScope.launch {
             tonight.value=getTonightFromDB()
        }
    }

    private suspend fun getTonightFromDB(): SleepNight? {
        return withContext(Dispatchers.IO){
            var night=database.getTonight()
            if (night?.endTimeMilli!=night?.startTimeMilli){
                night=null
            }
            night
        }
    }

    fun onStartTracking(){
        viewModelScope.launch {
            val newNight=SleepNight()
            insert(newNight)
            tonight.value=getTonightFromDB()
        }
    }

    private suspend fun insert(night: SleepNight){
        withContext(Dispatchers.IO){
            database.insert(night)
        }
    }

    fun onStopTracking(){
        viewModelScope.launch {
            val oldNight=tonight.value ?: return@launch
            oldNight.endTimeMilli= System.currentTimeMillis()
            update(oldNight)
        }
    }

    private suspend fun update(night:SleepNight){
        withContext(Dispatchers.IO){
            database.update(night)
        }
    }

    fun onClear(){
        viewModelScope.launch {
            clear()
            tonight.value=null
        }
    }

    suspend fun clear(){
        withContext(Dispatchers.IO){
            database.clear()
        }
    }
}