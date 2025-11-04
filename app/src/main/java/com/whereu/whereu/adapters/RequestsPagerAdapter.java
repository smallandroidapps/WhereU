package com.whereu.whereu.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.whereu.whereu.fragments.FromMeRequestsFragment;
import com.whereu.whereu.fragments.ToMeRequestsFragment;

import java.util.ArrayList;
import java.util.List;

public class RequestsPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragments = new ArrayList<>();

    public RequestsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
        fragments.add(new ToMeRequestsFragment());
        fragments.add(new FromMeRequestsFragment());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}
