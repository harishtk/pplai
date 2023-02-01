package com.aiavatar.app.commons.util

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import retrofit2.Call
import java.util.Date

abstract class NetworkBoundResource<ResultType, RequestType>(
    private val dispatcher: CoroutineDispatcher
) {

    // LiveData that represents the resource, implemented in the base class
    val result: MutableLiveData<Resource<ResultType>> = MutableLiveData()

    // Called to get the cached data from the database
    @WorkerThread
    abstract fun loadFromDb(): ResultType?

    // Called with the data in the database to determine when it was fetched
    // from the network
    @WorkerThread
    abstract fun getDataFetchDate(data: ResultType?): Date?

    // Called with the data in the database to decide whether it should be
    // fetched from the network
    @WorkerThread
    abstract fun shouldFetch(data: ResultType?, dataFetchDate: Date?): Boolean

    // Called to create the API call
    @WorkerThread
    abstract fun createCall(): Call<RequestType>

    // Called to save the result of the API response into the database
    @WorkerThread
    abstract fun saveCallResult(item: RequestType)

    @WorkerThread
    abstract fun shouldLogin(): Boolean

    @WorkerThread
    abstract fun autoReAuthenticate(data: ResultType?, dataFetchDate: Date?)

    @WorkerThread
    abstract fun loadFromNetwork(data: ResultType?, dataFetchDate: Date?)

    fun execute() {
        dispatcher.asExecutor().execute {
            val data = loadFromDb()
            val dataFetchDate = getDataFetchDate(data)

            if (/* isOnline() */ true) {
                result.postValue(Resource.loading(data))
                if (shouldFetch(data, dataFetchDate)) {
                    if (shouldLogin()) {
                        autoReAuthenticate(data, dataFetchDate)
                    } else {
                        loadFromNetwork(data, dataFetchDate)
                    }
                } else {
                    result.postValue(Resource.success(data, dataFetchDate))
                }
            } else {
                result.postValue(Resource.cached(data, dataFetchDate))
            }
        }
    }

}