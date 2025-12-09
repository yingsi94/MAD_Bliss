package com.example.chatbox.services;

import com.example.chatbox.models.MyResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("test")
    Call<MyResponse> testApi();

    @GET("message")
    Call<MyResponse> getMessage(@Query("msg") String userInput);
}
