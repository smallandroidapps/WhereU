package com.locationapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {

    private MutableLiveData<String> currentLocation = new MutableLiveData<>();

    public LiveData<String> getCurrentLocation() {
        return currentLocation;
    }

    public void updateLocation(String location) {
        currentLocation.setValue(location);
    }
}