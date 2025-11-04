package com.whereu.whereu.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class RequestsPagerAdapter extends FragmentStateAdapter {

    public RequestsPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PendingRequestsFragment();
            case 1:
                return new FromMeRequestsFragment();
            case 2:
                return new ToMeRequestsFragment();
            default:
                return new PendingRequestsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}