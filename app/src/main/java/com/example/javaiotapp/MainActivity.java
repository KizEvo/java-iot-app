package com.example.javaiotapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    ViewPager2 viewPager2;
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager2 = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        viewPager2.setAdapter(new FragmentAdapter(this));

        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0:
                        tab.setText("Home");
                        tab.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_home_24, null));
                        break;
                    case 1:
                        tab.setText("Status");
                        tab.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_local_parking_24, null));
                        break;
                    case 2:
                        tab.setText("Account");
                        tab.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_account_circle_24, null));
                        break;
                }
            }
        });
        tabLayoutMediator.attach();
    }


}