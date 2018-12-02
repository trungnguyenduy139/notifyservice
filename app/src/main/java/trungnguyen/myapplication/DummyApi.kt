package trungnguyen.myapplication

import io.reactivex.Observable
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by trungnd4 on 02/12/2018.
 */
interface DummyApi {

    @POST("abcd")
    fun callDummyApi(@Query("text") text: String,
                     @Query("title") title: String): Observable<BaseResponse>

}
