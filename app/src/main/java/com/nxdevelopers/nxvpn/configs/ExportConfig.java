package com.nxdevelopers.nxvpn.configs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;

import com.nxdevelopers.nxvpn.NxVpn;
import com.nxdevelopers.nxvpn.security.AppSecurityManager;
import com.nxdevelopers.nxvpn.R;

public class ExportConfig extends AppCompatActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static SharedPreferences app_prefs;

    private MaterialToolbar    export_toolbar;
    private MaterialButton     export_config_button;
    private TextInputEditText  config_name;
    private TextInputEditText  config_msg;

    // M3 MaterialSwitch menggantikan SwitchCompat
    private MaterialSwitch lock_config_from_edit;
    private MaterialSwitch lock_settings_app;
    private MaterialSwitch only_mobile_data;
    private MaterialSwitch validate_date;
    private MaterialSwitch lock_login;

    private long   mValidade              = 0;
    private String configToExportOnPermisson;

    private static final int CREATE_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_config);

        // M3 Toolbar
        export_toolbar = (MaterialToolbar) findViewById(R.id.export_toolbar);
        setSupportActionBar(export_toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        app_prefs = getSharedPreferences(NxVpn.PREFS_GERAL, Context.MODE_PRIVATE);

        // Bind views
        config_name          = (TextInputEditText) findViewById(R.id.config_file_name);
        config_msg           = (TextInputEditText) findViewById(R.id.config_msg);
        lock_config_from_edit = (MaterialSwitch)  findViewById(R.id.lock_config);
        lock_settings_app    = (MaterialSwitch)   findViewById(R.id.lock_config_settings);
        only_mobile_data     = (MaterialSwitch)   findViewById(R.id.lock_only_mobile_data);
        validate_date        = (MaterialSwitch)   findViewById(R.id.lock_config_validate);
        lock_login           = (MaterialSwitch)   findViewById(R.id.lock_login_edit);
        export_config_button = (MaterialButton)   findViewById(R.id.export_config_button);

        // Listeners
        lock_config_from_edit.setOnCheckedChangeListener(this);
        lock_settings_app.setOnCheckedChangeListener(this);
        only_mobile_data.setOnCheckedChangeListener(this);
        validate_date.setOnCheckedChangeListener(this);
        export_config_button.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------------------------------------
    // OnClickListener
    // -----------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.export_config_button) {
            String nameText = config_name.getText() != null
                    ? config_name.getText().toString() : "";

            if (nameText.isEmpty()) {
                Toast.makeText(this, getString(R.string.filename_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            if (lock_login.isChecked() && NxVpn.getUsuarioAndPass().isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_login), Toast.LENGTH_SHORT).show();
                return;
            }

            if (validate_date.isChecked()) {
                setValidadeDate();
            } else {
                mValidade = 0;
            }

            JSONObject config_str = new JSONObject();
            String k = AppSecurityManager.k1 + AppSecurityManager.k2
                    + AppSecurityManager.k3 + AppSecurityManager.k4 + AppSecurityManager.k5;
            String i = AppSecurityManager.iv1 + AppSecurityManager.iv2 + AppSecurityManager.ivf;

            try {
                config_str.put("appVersion",           22);
                config_str.put("ConfigValidityDate",   mValidade);
                config_str.put("ConnectionMode",        NxVpn.getConnectionMode());
                config_str.put("isHTTPDirect",          NxVpn.isHTTPDirect());
                config_str.put("isPayloadAfterTLS",     NxVpn.isPayloadAfterTLS());
                config_str.put("isLockConfig",          lock_config_from_edit.isChecked());
                config_str.put("isLockAppSettings",     lock_settings_app.isChecked());
                config_str.put("isEnableUdp",           NxVpn.isEnableCustomUDP());
                config_str.put("UDPAddr",               NxVpn.getUDPResolver());
                config_str.put("isEnableDNS",           NxVpn.isEnableCustomDNS());
                config_str.put("DNS1",                  NxVpn.customDNS1());
                config_str.put("DNS2",                  NxVpn.customDNS2());
                config_str.put("ConfigMsg",
                        config_msg.getText() != null ? config_msg.getText().toString() : "");
                config_str.put("onlyMobileData",        only_mobile_data.isChecked());
                config_str.put("TLSVersion",            NxVpn.getTLSVersion());
                config_str.put("CurrentPayload",        NxVpn.getPayloadKey());
                config_str.put("ServerSSH",             NxVpn.getServidorSSHDomain());
                config_str.put("ServerProxy",           NxVpn.getProxyIPDomain());
                config_str.put("ServerSNI",             NxVpn.getSNI());
                config_str.put("LockEditLogin",         lock_login.isChecked());
                config_str.put("ConfigAuthData",        NxVpn.getUsuarioAndPass());

                String enc_config = AppSecurityManager.encryptStrAndToBase64(
                        i, k, config_str.toString());
                configToExportOnPermisson = enc_config.replace("\n", "");

                Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory()
                        + "/Download");
                createFile(uri);

            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void createFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*text/plain");
        String fileName = (config_name.getText() != null
                ? config_name.getText().toString() : "config") + ".vpnlite";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        startActivityForResult(intent, CREATE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    Uri uri = data.getData();
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    outputStream.write(configToExportOnPermisson.getBytes());
                    outputStream.close();
                    Toast.makeText(this, getString(R.string.export_sucess), Toast.LENGTH_SHORT).show();
                    finish();
                } catch (IOException e) {
                    Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -----------------------------------------------------------------------
    // OnCheckedChangeListener
    // -----------------------------------------------------------------------

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (validate_date.isChecked()) {
            setValidadeDate();
        } else {
            mValidade = 0;
        }
    }

    // -----------------------------------------------------------------------
    // Date picker (logika asli dipertahankan, hanya style dialog M3)
    // -----------------------------------------------------------------------

    private void setValidadeDate() {
        Calendar c = Calendar.getInstance();
        final long time_hoje = c.getTimeInMillis();

        c.setTimeInMillis(time_hoje + (1000L * 60 * 60 * 24));
        int mYear  = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay   = c.get(Calendar.DAY_OF_MONTH);

        mValidade = c.getTimeInMillis();

        // Gunakan DATE_DIALOG_THEME yang sudah diubah ke M3 di themes.xml
        final DatePickerDialog dialog = new DatePickerDialog(
                this, R.style.DATE_DIALOG_THEME,
                (picker, year, monthOfYear, dayOfMonth) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, monthOfYear, dayOfMonth);
                    mValidade = cal.getTimeInMillis();
                },
                mYear, mMonth, mDay);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok),
                (dialog2, which) -> {
                    DateFormat df   = DateFormat.getDateInstance();
                    DatePicker  date = dialog.getDatePicker();
                    Calendar cal = Calendar.getInstance();
                    cal.set(date.getYear(), date.getMonth(), date.getDayOfMonth());
                    mValidade = cal.getTimeInMillis();

                    if (mValidade < time_hoje) {
                        mValidade = 0;
                        Toast.makeText(getApplicationContext(),
                                R.string.error_date_selected_invalid, Toast.LENGTH_SHORT).show();
                        if (validate_date != null) validate_date.setChecked(false);
                    } else {
                        long dias = (mValidade - time_hoje) / 1000 / 60 / 60 / 24;
                        if (validate_date != null) {
                            validate_date.setVisibility(View.VISIBLE);
                            validate_date.setText(
                                    String.format("%s (%s)", dias, df.format(mValidade)));
                        }
                    }
                });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                (d, which) -> {
                    mValidade = 0;
                    if (validate_date != null) validate_date.setChecked(false);
                });

        dialog.setOnCancelListener(v -> {
            mValidade = 0;
            if (validate_date != null) validate_date.setChecked(false);
        });

        dialog.show();
    }
}