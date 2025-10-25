package com.whereu.whereu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.whereu.whereu.models.LocationRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FrequentRequestCacheManager {

    private static final String PREF_NAME = "FrequentRequestPrefs";
    private static final String KEY_FREQUENT_REQUESTS = "frequentRequests";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public FrequentRequestCacheManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveFrequentRequests(List<LocationRequest> requests) {
        String json = gson.toJson(requests);
        sharedPreferences.edit().putString(KEY_FREQUENT_REQUESTS, json).apply();
    }

    public List<LocationRequest> getFrequentRequests() {
        String json = sharedPreferences.getString(KEY_FREQUENT_REQUESTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<LocationRequest>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
