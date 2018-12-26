package trungnguyen.myapplication

import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Created by trungnd4 on 02/12/2018.
 */
interface DummyApi {

    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("momo")
    fun callDummyApi(@Body content: RequestBody): Observable<BaseResponse>

}
