package com.example.pictobeads;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

/**
 * MainActivity serves as the root container for the sliding home screen.
 * It uses a ViewPager2 to navigate between the start menu and the projects gallery.
 */
public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pager);

        viewPager = findViewById(R.id.main_view_pager);
        viewPager.setAdapter(new MainPagerAdapter(this));
    }

    /**
     * Programmatically scrolls to the start screen.
     */
    public void goToStart() {
        viewPager.setCurrentItem(0, true);
    }

    /**
     * Programmatically scrolls to the projects gallery.
     */
    public void goToProjects() {
        viewPager.setCurrentItem(1, true);
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        MainPagerAdapter(FragmentActivity fa) { super(fa); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return (position == 0) ? new StartFragment() : new ProjectsFragment();
        }

        @Override
        public int getItemCount() { return 2; }
    }
}
