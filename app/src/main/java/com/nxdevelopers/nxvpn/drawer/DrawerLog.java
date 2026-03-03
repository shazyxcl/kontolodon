package com.nxdevelopers.nxvpn.drawer;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import com.nxdevelopers.nxvpn.R;

public class DrawerLog
        implements LogsAdapter.OnItemClickListener {

    private static final String TAG = DrawerLog.class.getSimpleName();

    private final Activity     mActivity;
    private final Handler      mHandler;
    private       DrawerLayout drawerLayout;
    private       RecyclerView drawerListView;
    private       LogsAdapter  mAdapter;

    public DrawerLog(Activity activity) {
        mActivity = activity;
        mHandler  = new Handler(Looper.getMainLooper());
    }

    // -----------------------------------------------------------------------
    // Setup Drawer + MaterialToolbar + ActionBarDrawerToggle (M3)
    // -----------------------------------------------------------------------

    public void setDrawer(DrawerLayout.DrawerListener listener) {
        // DrawerLayout utama (nested, untuk log panel di sisi kanan)
        drawerLayout   = mActivity.findViewById(R.id.drawerLayout);
        drawerListView = mActivity.findViewById(R.id.recyclerDrawerView);

        drawerLayout.addDrawerListener(listener);

        // M3: Setup ActionBarDrawerToggle agar hamburger icon muncul otomatis
        // DrawerLayoutMain adalah drawer kiri (NavigationView)
        DrawerLayout drawerLayout = mActivity.findViewById(R.id.drawerLayout);
        if (drawerLayout != null && mActivity instanceof AppCompatActivity) {
            AppCompatActivity    appCompat = (AppCompatActivity) mActivity;
            MaterialToolbar      toolbar   = mActivity.findViewById(R.id.app_toolbar);

            if (toolbar != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        mActivity,
                        drawerLayout,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);

                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            }
        }

        // Setup NavigationView (drawer kiri) — tutup saat item dipilih
     /**   NavigationView navigationView = mActivity.findViewById(R.id.drawerNavigationView);
        if (navigationView != null) {
            DrawerLayout drawerMain = mActivity.findViewById(R.id.drawerLayoutMain);
            navigationView.setNavigationItemSelectedListener(item -> {
                if (drawerMain != null) {
                    drawerMain.closeDrawer(GravityCompat.START);
                }
                return true;
            });
        }**/

        // Setup RecyclerView log
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        mAdapter = new LogsAdapter(layoutManager, mActivity);
        mAdapter.setOnItemClickListener(this);

        drawerListView.setAdapter(mAdapter);
        drawerListView.setLayoutManager(layoutManager);

        mAdapter.scrollToLastPosition();
    }

    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    public void clearLogs() {
        mAdapter.clearLog();
    }

    // -----------------------------------------------------------------------
    // LogsAdapter.OnItemClickListener
    // -----------------------------------------------------------------------

    @Override
    public void onItemClick(View view, int position, String logText) { }

    @Override
    public void onItemLongClick(View view, int position, String logText) {
        // Reserved: copy log ke clipboard
    }

    // -----------------------------------------------------------------------
    // Lifecycle events
    // -----------------------------------------------------------------------

    public void onResume() {
        mAdapter.setLogLevel(3);
    }

    public void onDestroy() {
        // Reserved: remove log listener
    }
}