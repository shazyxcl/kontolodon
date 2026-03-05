package com.nxdevelopers.nxvpn;

import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.nxdevelopers.nxvpn.drawer.DrawerLog;
import com.nxdevelopers.nxvpn.logs.AppLogManager;
import com.nxdevelopers.nxvpn.profile.EditProfileActivity;
import com.nxdevelopers.nxvpn.profile.ProfileAdapter;
import com.nxdevelopers.nxvpn.profile.ProfileManager;
import com.nxdevelopers.nxvpn.profile.VpnProfile;
import com.nxdevelopers.nxvpn.service.TunnelManager;
import com.nxdevelopers.nxvpn.settings.AppSettings;
import com.nxdevelopers.nxvpn.tunnel.TunnelUtils;

import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity
        implements DrawerLayout.DrawerListener, ProfileAdapter.ProfileAdapterListener {

    public static final int START_VPN_PROFILE     = 70;
    public static final int REQUEST_EDIT_PROFILE  = 71;
    public static final int REQUEST_IMPORT_PROFILE = 72;
    private static final String TAG = "MainActivity";

    public static SharedPreferences app_prefs;
    public static SharedPreferences start_msg;

    // Static refs dibutuhkan agar TunnelManager bisa update UI dari background thread
    @SuppressLint("StaticFieldLeak") public static MaterialButton start_button;
    @SuppressLint("StaticFieldLeak") public static TextView       tv_active_profile_label;

    public static Handler  UIHandler = new Handler(Looper.getMainLooper());
    public static Context  sContext;
    public static Activity sActivity;
    public static PowerManager.WakeLock wakeLock;

    // Root view untuk Snackbar
    private View rootView;

    public static void runOnUI(Runnable r) {
        UIHandler.post(r);
    }

    // -----------------------------------------------------------------------
    // Static status callback (dipanggil TunnelManager dari background thread)
    // Fungsional dipertahankan; hanya tipe start_button diubah ke MaterialButton
    // -----------------------------------------------------------------------

    public static void setButtonStatus(final String status) {
        switch (status) {
            case "INICIANDO":
            case "CONECTANDO":
            case "CONECTADO":
                runOnUI(() -> {
                    start_button.setText(R.string.stop);
                    start_button.setEnabled(true);
                });
                break;

            case "DESCONECTADO":
                runOnUI(() -> {
                    start_button.setText(R.string.start);
                    start_button.setEnabled(true);
                });
                break;

            case "PARANDO":
                runOnUI(() -> {
                    start_button.setText(R.string.stopping);
                    start_button.setEnabled(false);
                });
                break;
        }
    }

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    private Thread         mTunnelThread;
    private TunnelManager  mTunnelManager;
    private DrawerLog      mDrawer;
    private MaterialToolbar app_toolbar;   // M3: MaterialToolbar
    private RecyclerView   rvProfiles;
    private ProfileAdapter profileAdapter;
    private ProfileManager profileManager;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startApp();
    }

    private void startApp() {
        mDrawer = new DrawerLog(this);
        setContentView(R.layout.activity_main_drawer);

        // M3: MaterialToolbar (dari toolbar_main.xml / activity_main_drawer.xml)
        app_toolbar = (MaterialToolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(app_toolbar);
        app_toolbar.requestFocus();

        rootView       = findViewById(android.R.id.content);
        sActivity      = this;
        sContext       = this;
        profileManager = ProfileManager.getInstance(this);

        mDrawer.setDrawer(this);

        app_prefs = getSharedPreferences(NxVpn.PREFS_GERAL, Context.MODE_PRIVATE);
        start_msg = getSharedPreferences(NxVpn.FIRST_START,  Context.MODE_PRIVATE);

        boolean showFirst = start_msg.getBoolean("default_config", true);
        Intent  intent    = getIntent();
        if (showFirst && (intent == null || !intent.getBooleanExtra("IS_IMPORT", false))) {
            start_msg.edit().putBoolean("default_config", false).apply();
            showStartMsg();
        }

        // Bind views — M3: Button → MaterialButton
        start_button            = (MaterialButton)  findViewById(R.id.activity_StartConnection);
        tv_active_profile_label = (TextView)         findViewById(R.id.tv_active_profile_label);
        rvProfiles              = (RecyclerView)     findViewById(R.id.rv_profiles);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_profile);

        // RecyclerView
        rvProfiles.setLayoutManager(new LinearLayoutManager(this));
        VpnProfile active = profileManager.getActiveProfile();
        profileAdapter = new ProfileAdapter(
                profileManager.getProfiles(),
                active != null ? active.getId() : null,
                this);
        rvProfiles.setAdapter(profileAdapter);

        // Listeners
        start_button.setOnClickListener(v -> handleStartStop());
        fab.setOnClickListener(v -> openEditProfile(null));

        refreshStartButton();
        refreshActiveProfileLabel();

        WifiManager wifi = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if ((wifi != null && wifi.isWifiEnabled()) || TunnelManager.isServiceRunning) {
            // reserved for future use
        }
    }

    // -----------------------------------------------------------------------
    // ProfileAdapter.ProfileAdapterListener
    // -----------------------------------------------------------------------

    @Override
    public void onProfileSelected(VpnProfile profile) {
        profileManager.setActiveProfile(profile.getId());
        profileAdapter.setActiveProfileId(profile.getId());
        refreshActiveProfileLabel();
        // M3: Snackbar menggantikan Toast untuk aksi konfirmasi
        Snackbar.make(rootView,
                getString(R.string.profile_selected, profile.getDisplayName()),
                Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onEditProfile(VpnProfile profile) {
        openEditProfile(profile.getId());
    }

    @Override
    public void onDeleteProfile(final VpnProfile profile) {
        // M3: MaterialAlertDialogBuilder menggantikan AlertDialog.Builder
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_profile_title)
                .setMessage(getString(R.string.delete_profile_confirm, profile.getDisplayName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    profileManager.deleteProfile(profile.getId());
                    VpnProfile nowActive = profileManager.getActiveProfile();
                    profileAdapter.setActiveProfileId(
                            nowActive != null ? nowActive.getId() : null);
                    profileAdapter.notifyDataSetChanged();
                    refreshActiveProfileLabel();
                })
                .show();
    }

    @Override
    public void onExportProfile(VpnProfile profile) {
        String json = profileManager.exportProfileJson(profile.getId());
        if (json == null) {
            Snackbar.make(rootView, R.string.export_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.export_profile_subject, profile.getDisplayName()));
        share.putExtra(Intent.EXTRA_TEXT, json);
        startActivity(Intent.createChooser(share, getString(R.string.export_profile_chooser)));
    }

    // -----------------------------------------------------------------------
    // VPN Start / Stop
    // -----------------------------------------------------------------------

    private void handleStartStop() {
        if (getString(R.string.stop).equals(start_button.getText().toString())) {
            stopVPNService();
        } else {
            VpnProfile active = profileManager.getActiveProfile();
            if (active == null) {
                showNoProfileDialog();
                return;
            }
            applyProfileToPrefs(active);
            startVPNService();
            Log.d(TAG, "Start VPN with profile: " + active.getDisplayName());
           // AppLogManager.addLog("Start VPN with profile: " + active.getDisplayName());
        }
    }

    private void applyProfileToPrefs(VpnProfile p) {
        app_prefs.edit()
                .putString("SSH_SERVER_DOMAIN",             p.getSshServerDomain())
                .putString("PROXY_IP_DOMAIN",               p.getProxyIpDomain())
                .putString("CUSTOM_SNI",                    p.getSni())
                .putString("PAYLOAD_KEY",                   p.getPayloadKey())
                .putString("SSH_AUTH_DATA",                 p.getSshAuthData())
                .putString("CONNECTION_MODE",               p.getConnectionMode())
                .putBoolean("IS_HTTP_DIRECT",               p.isHttpDirect())
                .putBoolean("PAYLOAD_AFTER_TLS",            p.isPayloadAfterTls())
                .putBoolean("IS_CUSTOM_FILE_LOCKED",        p.isCustomFileLocked())
                .putBoolean("IS_CUSTOM_FILE_LOCK_SETTINGS", p.isLockSettings())
                .putBoolean("IS_LOCK_LOGIN_EDIT",           p.isLockLoginEdit())
                .putBoolean("IS_CONFIG_ONLY_MOBILE_DATA",   p.isOnlyMobileData())
                .putLong("VALIDADE_CONFIG",                 p.getValidadeMillis())
                .putString("CONFIG_MSG",                    p.getConfigMsg())
                .apply();
    }

    private void showNoProfileDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.no_active_profile_title)
                .setMessage(R.string.no_active_profile_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.create_profile,
                        (dialog, which) -> openEditProfile(null))
                .show();
    }

    private void startVPNService() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.error_request_permission, Toast.LENGTH_LONG).show();
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }

    private void stopVPNService() {
        try {
            TunnelManager.stopForwarderSocks();
            TunnelManager.isToStopService = true;
            mTunnelManager = new TunnelManager();
            mTunnelThread  = new Thread(mTunnelManager);
            mTunnelThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMPORT_PROFILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importProfileFromUri(uri);
            }
        }

        if (requestCode == START_VPN_PROFILE && resultCode == Activity.RESULT_OK) {
            if (checkConfig()) {
                app_prefs.edit().putBoolean("LAST_A", false).apply();
                AppLogManager.clearLog();
                if (NxVpn.isShowLogs()) showLog();

                if (NxVpn.isEnableWakeLock()) {
                    try {
                        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                                .newWakeLock(1, "SecondVPNLite::WakeLock");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    TunnelManager.isToStopService = false;
                    TunnelManager.stoppedLog      = false;
                    TunnelManager.stoppingLog     = false;
                    TunnelManager.vpnDestroyedLog = false;
                    mTunnelManager = new TunnelManager();
                    mTunnelThread  = new Thread(mTunnelManager);
                    mTunnelThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, R.string.empty_configs, Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == RESULT_OK) {
            VpnProfile nowActive = profileManager.getActiveProfile();
            profileAdapter.setActiveProfileId(nowActive != null ? nowActive.getId() : null);
            profileAdapter.notifyDataSetChanged();
            refreshActiveProfileLabel();
        }
    }

    // -----------------------------------------------------------------------
    // Config validation (logika asli dipertahankan sepenuhnya)
    // -----------------------------------------------------------------------

    private boolean checkConfig() {
        if (!TunnelUtils.isNetworkOnline(this)) {
            Snackbar.make(rootView, R.string.no_network, Snackbar.LENGTH_SHORT).show();
            return false;
        }
        VpnProfile p = profileManager.getActiveProfile();
        if (p == null) return false;

        try {
            String[] parts = p.getSshServerDomain().split(":");
            if (parts[0].isEmpty() || Integer.parseInt(parts[1]) == 0) return false;
        } catch (Exception e) {
            return false;
        }

        try {
            String[] auth = p.getSshAuthData().split("@");
            if (auth[0].isEmpty() || auth[1].isEmpty()) return false;
        } catch (Exception e) {
            return false;
        }

        if ("MODO_HTTP".equals(p.getConnectionMode()) && !p.isHttpDirect()) {
            try {
                String[] parts = p.getProxyIpDomain().split(":");
                if (parts[0].isEmpty() || Integer.parseInt(parts[1]) == 0) return false;
            } catch (Exception e) {
                return false;
            }
        }

        if ("MODO_HTTPS".equals(p.getConnectionMode()) && p.getSni().isEmpty()) return false;

        if (p.isCustomFileLocked()) {
            if (p.isOnlyMobileData()) {
                WifiManager wm = (WifiManager) getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                if (wm != null && wm.isWifiEnabled()) {
                    Snackbar.make(rootView, R.string.only_mobile_data_toast,
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
            }
            if (checkValidade(p.getValidadeMillis())) {
                Snackbar.make(rootView, R.string.file_validate_end,
                        Snackbar.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    public static boolean checkValidade(long validadeDateMillis) {
        if (validadeDateMillis == 0) return false;
        return Calendar.getInstance().getTime().getTime() >= validadeDateMillis;
    }

    // -----------------------------------------------------------------------
    // Navigation helpers
    // -----------------------------------------------------------------------

    private void openEditProfile(String profileId) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        if (profileId != null) {
            intent.putExtra(EditProfileActivity.EXTRA_PROFILE_ID, profileId);
        }
        startActivityForResult(intent, REQUEST_EDIT_PROFILE);
    }

    private void showLog() {
        if (mDrawer != null && !isFinishing()) {
            DrawerLayout dl = mDrawer.getDrawerLayout();
            if (!dl.isDrawerOpen(GravityCompat.START)) {
                dl.openDrawer(GravityCompat.START);
            }
        }
    }

    // -----------------------------------------------------------------------
    // UI refresh
    // -----------------------------------------------------------------------

    private void refreshStartButton() {
        if (TunnelManager.isServiceRunning) {
            start_button.setText(R.string.stop);
        } else {
            start_button.setText(R.string.start);
        }
        start_button.setEnabled(true);
    }

    private void refreshActiveProfileLabel() {
        if (tv_active_profile_label == null) return;
        VpnProfile active = profileManager.getActiveProfile();
        if (active != null) {
            tv_active_profile_label.setText(
                    getString(R.string.active_profile_label, active.getDisplayName()));
        } else {
            tv_active_profile_label.setText(R.string.no_active_profile_hint);
        }
    }

    // -----------------------------------------------------------------------
    // Dialogs / permissions
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Import Profile
    // -----------------------------------------------------------------------

    /**
     * Shows a dialog letting the user choose between picking a JSON file
     * or pasting raw JSON text.
     */
    private void showImportDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_profile_title)
                .setItems(new String[]{
                        getString(R.string.import_from_file),
                        getString(R.string.import_from_text)
                }, (dialog, which) -> {
                    if (which == 0) {
                        openImportFilePicker();
                    } else {
                        showImportTextDialog();
                    }
                })
                .show();
    }

    /** Opens the system file picker filtered to JSON / plain-text files. */
    private void openImportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.import_file_chooser)),
                    REQUEST_IMPORT_PROFILE);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(rootView, R.string.no_file_manager, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Shows a multi-line EditText dialog so the user can paste JSON directly. */
    private void showImportTextDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.import_paste_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(4);
        input.setMaxLines(10);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_from_text)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_action, (dialog, which) -> {
                    String json = input.getText().toString().trim();
                    if (!json.isEmpty()) {
                        handleImportJson(json);
                    }
                })
                .show();
    }

    /** Reads the file content from the given URI and attempts to import it. */
    private void importProfileFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Snackbar.make(rootView, R.string.import_failed, Snackbar.LENGTH_SHORT).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            handleImportJson(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(rootView, R.string.import_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Parses the JSON string as a VpnProfile (single or array),
     * saves it, and refreshes the list.
     */
    private void handleImportJson(String json) {
        try {
            int importedCount = 0;

            // Support both a single JSON object and a JSON array of profiles
            if (json.trim().startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    VpnProfile p = profileManager.importProfileJson(arr.getJSONObject(i).toString());
                    if (p != null) importedCount++;
                }
            } else {
                VpnProfile p = profileManager.importProfileJson(json);
                if (p != null) importedCount = 1;
            }

            if (importedCount > 0) {
                profileAdapter.notifyDataSetChanged();
                refreshActiveProfileLabel();
                String msg = getResources().getQuantityString(
                        R.plurals.import_success, importedCount, importedCount);
                Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(rootView, R.string.import_failed, Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(rootView, R.string.import_invalid_json, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showStartMsg() {
        // M3: MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.start_msg_title)
                .setCancelable(true)
                .setMessage(R.string.start_msg_text)
                .setPositiveButton("OK", (dialog, which) -> {
                    app_prefs.edit().putBoolean("default_config", false).apply();
                    dialog.dismiss();
                    getPermissionAndroid13();
                })
                .show();
    }

    public void getPermissionAndroid13() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Menu
    // -----------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            if (NxVpn.getIsCustomFileLockSettings()) {
                Snackbar.make(rootView, R.string.block_by_config_author,
                        Snackbar.LENGTH_LONG).show();
            } else {
                startActivity(new Intent(this, AppSettings.class));
            }
        } else if (id == R.id.import_profile) {
            showImportDialog();
        } else if (id == R.id.minimize) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } else if (id == R.id.exit_app) {
            finishAffinity();
        }
        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------------------------------------
    // Lifecycle overrides
    // -----------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStartButton();
        refreshActiveProfileLabel();
        profileAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // -----------------------------------------------------------------------
    // DrawerLayout.DrawerListener
    // -----------------------------------------------------------------------

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) { }

    @Override
    public void onDrawerStateChanged(int newState) { }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        if (drawerView.getId() == R.id.activity_mainLogsDrawerLinear) {
            app_toolbar.getMenu().clear();
            getMenuInflater().inflate(R.menu.main_menu, app_toolbar.getMenu());
        }
    }
}