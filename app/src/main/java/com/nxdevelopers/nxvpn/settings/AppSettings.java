package com.nxdevelopers.nxvpn.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.nxdevelopers.nxvpn.MainActivity;
import com.nxdevelopers.nxvpn.NxVpn;
import com.nxdevelopers.nxvpn.R;

public class AppSettings extends AppCompatActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static SharedPreferences app_prefs;

    private static final String TAG = "AppSettings";

    // Toolbar
    private MaterialToolbar settings_toolbar;

    // Switches — diganti MaterialSwitch (Material You)
    private MaterialSwitch enable_udp;
    private MaterialSwitch enable_dns;
    private MaterialSwitch enable_tethering;
    private MaterialSwitch enable_wakelock;
    private MaterialSwitch show_logs;
    private MaterialSwitch enable_ssh_compress;
    private MaterialSwitch disable_tcp_delay;

    // TextInputEditText (M3 outlined)
    private TextInputEditText type_udp;
    private TextInputEditText type_dns_1;
    private TextInputEditText type_dns_2;

    // TextInputLayout wrapper (untuk show/hide field)
    private TextInputLayout layout_udp;
    private TextInputLayout layout_dns_1;
    private TextInputLayout layout_dns_2;

    // Clickable text views
    private TextView tls_version;
    private TextView filter_apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        // M3 MaterialToolbar
        settings_toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(settings_toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        settings_toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        app_prefs = getSharedPreferences(NxVpn.PREFS_GERAL, Context.MODE_PRIVATE);

        // Bind switches
        enable_udp       = (MaterialSwitch) findViewById(R.id.enable_udp);
        enable_dns       = (MaterialSwitch) findViewById(R.id.enable_dns);
        enable_tethering = (MaterialSwitch) findViewById(R.id.enable_thetering);
        enable_wakelock  = (MaterialSwitch) findViewById(R.id.enable_wakelock);
        show_logs        = (MaterialSwitch) findViewById(R.id.show_logs);
        enable_ssh_compress = (MaterialSwitch) findViewById(R.id.enable_sshcompress);
        disable_tcp_delay   = (MaterialSwitch) findViewById(R.id.disable_tcp_delay);

        // Bind text inputs
        tls_version  = (TextView)          findViewById(R.id.set_tls_version);
        filter_apps  = (TextView)          findViewById(R.id.filter_apps_configure);
        type_udp     = (TextInputEditText) findViewById(R.id.input_udp);
        type_dns_1   = (TextInputEditText) findViewById(R.id.input_dns_1);
        type_dns_2   = (TextInputEditText) findViewById(R.id.input_dns_2);

        // Bind wrappers TextInputLayout (untuk kontrol visibility)
        // ID layout_udp, layout_dns_1, layout_dns_2 perlu ada di XML
        // Jika tidak ada ID wrapper, fallback ke view edittext itu sendiri
        layout_udp   = findLayoutById(R.id.layout_input_udp);
        layout_dns_1 = findLayoutById(R.id.layout_input_dns_1);
        layout_dns_2 = findLayoutById(R.id.layout_input_dns_2);

        // Set clickable for TextViews
        tls_version.setOnClickListener(this);
        filter_apps.setOnClickListener(this);

        // Populate UI from prefs
        populateSwitches();

        // Attach listeners
        enable_udp.setOnCheckedChangeListener(this);
        enable_dns.setOnCheckedChangeListener(this);
        enable_tethering.setOnCheckedChangeListener(this);
        enable_wakelock.setOnCheckedChangeListener(this);
        show_logs.setOnCheckedChangeListener(this);
        enable_ssh_compress.setOnCheckedChangeListener(this);
        disable_tcp_delay.setOnCheckedChangeListener(this);
    }

    /** Helper — cari TextInputLayout; kembalikan null jika tidak ada */
    private TextInputLayout findLayoutById(int id) {
        View v = findViewById(id);
        return (v instanceof TextInputLayout) ? (TextInputLayout) v : null;
    }

    /** Helper — set visibility ke view atau fallbackView */
    private void setFieldVisibility(TextInputLayout layout, TextInputEditText fallback, int visibility) {
        if (layout != null) {
            layout.setVisibility(visibility);
        } else if (fallback != null) {
            fallback.setVisibility(visibility);
        }
    }

    private void populateSwitches() {
        if (NxVpn.isEnableCustomUDP()) {
            enable_udp.setChecked(true);
            setFieldVisibility(layout_udp, type_udp, View.VISIBLE);
            type_udp.setText(NxVpn.getUDPResolver());
        } else {
            enable_udp.setChecked(false);
            setFieldVisibility(layout_udp, type_udp, View.GONE);
        }

        if (NxVpn.isEnableCustomDNS()) {
            enable_dns.setChecked(true);
            setFieldVisibility(layout_dns_1, type_dns_1, View.VISIBLE);
            setFieldVisibility(layout_dns_2, type_dns_2, View.VISIBLE);
            type_dns_1.setText(NxVpn.customDNS1());
            type_dns_2.setText(NxVpn.customDNS2());
        } else {
            enable_dns.setChecked(false);
            setFieldVisibility(layout_dns_1, type_dns_1, View.GONE);
            setFieldVisibility(layout_dns_2, type_dns_2, View.GONE);
        }

        enable_tethering.setChecked(NxVpn.isEnableTethering());
        enable_wakelock.setChecked(NxVpn.isEnableWakeLock());
        show_logs.setChecked(NxVpn.isShowLogs());
        enable_ssh_compress.setChecked(NxVpn.isEnableSSHCompress());
        disable_tcp_delay.setChecked(NxVpn.isEnableNoTCPDelay());
    }

    // -----------------------------------------------------------------------
    // OnClickListener
    // -----------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.set_tls_version) {
            // M3 MaterialAlertDialogBuilder menggantikan AlertDialog.Builder lama
            final String[] tlsOptions = {"Auto", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
            final String[] tlsValues  = {"auto", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.tls_version_title)
                    .setItems(tlsOptions, (dialog, which) ->
                            app_prefs.edit()
                                    .putString("TLS_VERSION", tlsValues[which])
                                    .apply())
                    .show();

        } else if (id == R.id.filter_apps_configure) {
            try {
                Intent intent = new Intent(this, AllowedAppsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.d("Settings", "Falhou ao iniciar allowed apps " + e);
                e.printStackTrace();
            }
        }
    }

    // -----------------------------------------------------------------------
    // OnCheckedChangeListener
    // -----------------------------------------------------------------------

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Logika asli dipertahankan
        if (!enable_udp.isChecked()) {
            enable_dns.setChecked(true);
        } else if (enable_udp.isChecked() &&
                type_udp.getText() != null &&
                type_udp.getText().toString().isEmpty()) {
            type_udp.setText("127.0.0.1:7300");
        }

        if (enable_dns.isChecked() &&
                type_dns_1.getText() != null &&
                type_dns_1.getText().toString().isEmpty()) {
            type_dns_1.setText("1.1.1.1");
        }
        if (enable_dns.isChecked() &&
                type_dns_2.getText() != null &&
                type_dns_2.getText().toString().isEmpty()) {
            type_dns_2.setText("1.0.0.1");
        }

        savePreferences(false);
    }

    // -----------------------------------------------------------------------
    // Navigation & Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        savePreferences(true);
        Intent main_activity = new Intent(this, MainActivity.class);
        main_activity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        this.startActivity(main_activity);
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        savePreferences(true);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        savePreferences(true);
        super.onDestroy();
    }

    // -----------------------------------------------------------------------
    // Save preferences (logika asli dipertahankan sepenuhnya)
    // -----------------------------------------------------------------------

    private void savePreferences(boolean isBackToHome) {
        // --- UDP ---
        if (enable_udp.isChecked()) {
            String udp_text = type_udp.getText() != null ? type_udp.getText().toString() : "";
            enable_udp.setChecked(true);
            setFieldVisibility(layout_udp, type_udp, View.VISIBLE);
            if (!isBackToHome) {
                if (!udp_text.isEmpty()) {
                    app_prefs.edit().putString("UDP_ADDR", udp_text).apply();
                    type_udp.setText(NxVpn.getUDPResolver());
                }
            } else {
                if (udp_text.isEmpty()) {
                    app_prefs.edit().putBoolean("IS_ENABLE_UDP", false).apply();
                } else {
                    app_prefs.edit().putBoolean("IS_ENABLE_UDP", true).apply();
                    app_prefs.edit().putString("UDP_ADDR", udp_text).apply();
                }
            }
        } else {
            if (!isBackToHome) {
                enable_udp.setChecked(false);
                setFieldVisibility(layout_udp, type_udp, View.GONE);
            } else {
                app_prefs.edit().putBoolean("IS_ENABLE_UDP", false).apply();
            }
        }

        // --- DNS ---
        if (enable_dns.isChecked()) {
            String dns_1_text = type_dns_1.getText() != null ? type_dns_1.getText().toString() : "";
            String dns_2_text = type_dns_2.getText() != null ? type_dns_2.getText().toString() : "";
            enable_dns.setChecked(true);
            setFieldVisibility(layout_dns_1, type_dns_1, View.VISIBLE);
            setFieldVisibility(layout_dns_2, type_dns_2, View.VISIBLE);
            if (!isBackToHome) {
                if (!dns_1_text.isEmpty()) {
                    app_prefs.edit().putString("CUSTOM_DNS_1", dns_1_text).apply();
                    type_dns_1.setText(NxVpn.customDNS1());
                } else if (!dns_2_text.isEmpty()) {
                    app_prefs.edit().putString("CUSTOM_DNS_2", dns_2_text).apply();
                    type_dns_2.setText(NxVpn.customDNS2());
                }
            } else {
                if (dns_1_text.isEmpty() && dns_2_text.isEmpty()) {
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS", false).apply();
                } else if (dns_2_text.isEmpty()) {
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS", false).apply();
                } else {
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS", true).apply();
                    app_prefs.edit().putString("CUSTOM_DNS_1", dns_1_text).apply();
                    app_prefs.edit().putString("CUSTOM_DNS_2", dns_2_text).apply();
                }
            }
        } else {
            if (!isBackToHome) {
                enable_dns.setChecked(false);
                setFieldVisibility(layout_dns_1, type_dns_1, View.GONE);
                setFieldVisibility(layout_dns_2, type_dns_2, View.GONE);
            } else {
                app_prefs.edit().putBoolean("IS_ENABLE_DNS", false).apply();
            }
        }

        // --- Tethering ---
        app_prefs.edit().putBoolean("IS_ENABLE_TETHERING", enable_tethering.isChecked()).apply();

        // --- Wake Lock ---
        app_prefs.edit().putBoolean("IS_ENABLE_WAKELOCK", enable_wakelock.isChecked()).apply();

        // --- Show Logs ---
        app_prefs.edit().putBoolean("IS_SHOW_LOGS", show_logs.isChecked()).apply();

        // --- SSH Compress ---
        app_prefs.edit().putBoolean("IS_ENABLE_SSH_COMPRESS", enable_ssh_compress.isChecked()).apply();

        // --- No TCP Delay ---
        app_prefs.edit().putBoolean("IS_ENABLE_NO_TCP_DELAY", disable_tcp_delay.isChecked()).apply();
    }
}