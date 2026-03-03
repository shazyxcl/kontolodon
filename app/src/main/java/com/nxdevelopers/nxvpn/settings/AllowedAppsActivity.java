package com.nxdevelopers.nxvpn.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import com.nxdevelopers.nxvpn.R;

import java.util.List;

public class AllowedAppsActivity extends AppCompatActivity {

    private RecyclerView    splitTunnelRV;
    private AppListAdapter  appAdapter;
    private MaterialButton  selectAllBtn;
    private MaterialButton  deselectAllBtn;
    private List<ResolveInfo> resolveInfoList;
    private PackageManager  packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allowed_app);
        initializeViews();
        setAdapterPackageManager();
        // M3: Status bar color diatur otomatis oleh tema Material You (DayNight)
        // Tidak perlu setStatusBar() manual lagi
    }

    private void setAdapterPackageManager() {
        appAdapter = new AppListAdapter(resolveInfoList, packageManager, AllowedAppsActivity.this);
        splitTunnelRV.setAdapter(appAdapter);
    }

    private void initializeViews() {
        // M3 MaterialToolbar
        MaterialToolbar toolbar = findViewById(R.id.app_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.title_filter_apps);
            }
            toolbar.setNavigationOnClickListener(v ->
                    getOnBackPressedDispatcher().onBackPressed());
        }

        splitTunnelRV = findViewById(R.id.splitRecyclerView);
        selectAllBtn  = findViewById(R.id.btnSelectAll);
        deselectAllBtn = findViewById(R.id.btnDeselectAll);

        packageManager  = getPackageManager();
        resolveInfoList = getInstalledApps(packageManager);

        selectAllBtn.setOnClickListener(v -> {
            appAdapter.selectAll();
            showToast(getString(R.string.block_all));
            appAdapter.notifyDataSetChanged();
        });

        deselectAllBtn.setOnClickListener(v -> {
            appAdapter.deselectAll();
            showToast(getString(R.string.unblock_all));
            appAdapter.notifyDataSetChanged();
        });
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private List<ResolveInfo> getInstalledApps(PackageManager pm) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return pm.queryIntentActivities(intent, 0);
    }

    /**
     * Tetap disediakan untuk kompatibilitas backward jika dipanggil dari tempat lain,
     * namun pada Material You status bar color dikelola secara otomatis oleh tema.
     */
    @Deprecated
    public static void setStatusBar(Activity activity, int color) {
        // Material You mengelola warna status bar secara otomatis via colorSurface
        // Method ini dipertahankan agar tidak break kode yang memanggilnya
    }
}
