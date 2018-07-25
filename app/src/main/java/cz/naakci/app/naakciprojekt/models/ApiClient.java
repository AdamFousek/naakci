package cz.naakci.app.naakciprojekt.models;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

/**
 * Created by adamfousek on 28.11.17.
 */

public interface ApiClient {

    // Retrofit - ziskání všech událostí
    @GET("api/v1/event/")
    Call<User> getUser(@Header("Authorization") String authHeader);

    @GET("api/v1/event/{id}/code/")
    Call<Tickets> getCodes(@Header("Authorization") String authHeader, @Path("id") int id);

    @GET("api/v1/event/{eventId}/code/{code}/validate/")
    Call<Code> checkCode(@Header("Authorization") String authHeader, @Path("eventId") int eventId, @Path("code") String code);

}
