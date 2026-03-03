package com.nxdevelopers.nxvpn.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.nxdevelopers.nxvpn.R;

public class EditProfileActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener {

    public static final String EXTRA_PROFILE_ID = "profile_id";

    // -----------------------------------------------------------------------
    // Views
    // -----------------------------------------------------------------------
    private MaterialToolbar      toolbarEditProfile;
    private TextInputEditText    etProfileName;
    private MaterialRadioButton  rbModeHttp;
    private MaterialRadioButton  rbModeHttps;
    private RadioGroup           rgConnectionMode;
    private MaterialSwitch       swDirectMode;
    private MaterialSwitch       swPayloadAfterTls;
    private TextInputEditText    etSshServer;

    // layout_proxy_server di XML baru adalah TextInputLayout, bukan LinearLayout
    // Kita gunakan TextInputLayout sebagai container
    private TextInputLayout      layoutProxyServer;
    private TextInputEditText    etProxyServer;
    private TextInputLayout      layoutSni;
    private TextInputEditText    etSni;
    private TextInputLayout      layoutPayload;
    private TextInputEditText    etPayload;
    private MaterialCardView     layoutUserPass;
    private TextInputEditText    etUserPass;
    private MaterialCardView     configMsgLayout;
    private TextView             configMsgTextview;
    private MaterialButton       btnSaveProfile;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private VpnProfile     profile;
    private ProfileManager profileManager;
    private boolean        isNewProfile;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileManager = ProfileManager.getInstance(this);

        bindViews();
        setupToolbar();
        resolveProfile();
        populateForm();
        attachListeners();
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
    // Initialisation
    // -----------------------------------------------------------------------

    private void bindViews() {
        toolbarEditProfile = (MaterialToolbar)      findViewById(R.id.toolbar_edit_profile);
        etProfileName      = (TextInputEditText)    findViewById(R.id.et_profile_name);
        rbModeHttp         = (MaterialRadioButton)  findViewById(R.id.rb_mode_http);
        rbModeHttps        = (MaterialRadioButton)  findViewById(R.id.rb_mode_https);
        rgConnectionMode   = (RadioGroup)           findViewById(R.id.activity_mainMetodoConexaoRadio);
        swDirectMode       = (MaterialSwitch)       findViewById(R.id.sw_direct_mode);
        swPayloadAfterTls  = (MaterialSwitch)       findViewById(R.id.sw_payload_after_tls);
        etSshServer        = (TextInputEditText)    findViewById(R.id.et_ssh_server);
        layoutProxyServer  = (TextInputLayout)      findViewById(R.id.layout_proxy_server);
        etProxyServer      = (TextInputEditText)    findViewById(R.id.et_proxy_server);
        layoutSni          = (TextInputLayout)      findViewById(R.id.layout_sni);
        etSni              = (TextInputEditText)    findViewById(R.id.et_sni);
        layoutPayload      = (TextInputLayout)      findViewById(R.id.layout_payload);
        etPayload          = (TextInputEditText)    findViewById(R.id.et_payload);
        layoutUserPass     = (MaterialCardView)     findViewById(R.id.layout_user_pass);
        etUserPass         = (TextInputEditText)    findViewById(R.id.et_user_pass);
        configMsgLayout    = (MaterialCardView)     findViewById(R.id.config_msg_layout);
        configMsgTextview  = (TextView)             findViewById(R.id.config_msg_textview);
        btnSaveProfile     = (MaterialButton)       findViewById(R.id.btn_save_profile);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarEditProfile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void resolveProfile() {
        String profileId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        if (profileId != null) {
            profile = profileManager.findById(profileId);
        }
        if (profile == null) {
            profile      = new VpnProfile();
            isNewProfile = true;
        }

        String title = isNewProfile
                ? getString(R.string.create_profile)
                : getString(R.string.edit_profile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void populateForm() {
        etProfileName.setText(profile.getName());
        etSshServer.setText(profile.getSshServerDomain());
        etProxyServer.setText(profile.getProxyIpDomain());
        etSni.setText(profile.getSni());
        etPayload.setText(profile.getPayloadKey());
        etUserPass.setText(profile.getSshAuthData());

        boolean isHttps = "MODO_HTTPS".equals(profile.getConnectionMode());
        rbModeHttps.setChecked(isHttps);
        rbModeHttp.setChecked(!isHttps);

        swDirectMode.setChecked(profile.isHttpDirect());
        swPayloadAfterTls.setChecked(profile.isPayloadAfterTls());

        if (profile.getConfigMsg() != null && !profile.getConfigMsg().isEmpty()) {
            configMsgTextview.setText(profile.getConfigMsg());
            configMsgLayout.setVisibility(View.VISIBLE);
        }

        if (profile.isCustomFileLocked()) {
            applyLockedFileMask();
        }

        updateDynamicVisibility();
    }

    private void applyLockedFileMask() {
        etSshServer.setEnabled(false);
        etSshServer.setText(profile.getSshServerDomain().replaceAll(".", "*"));

        etProxyServer.setEnabled(false);
        etProxyServer.setText(profile.getProxyIpDomain().replaceAll(".", "*"));

        etSni.setEnabled(false);
        etSni.setText(profile.getSni().replaceAll(".", "*"));

        etPayload.setEnabled(false);
        etPayload.setText(profile.getPayloadKey().replaceAll(".", "*"));

        rbModeHttp.setEnabled(false);
        rbModeHttps.setEnabled(false);
        swDirectMode.setEnabled(false);
        swPayloadAfterTls.setEnabled(false);

        if (profile.isLockLoginEdit()) {
            etUserPass.setEnabled(false);
            etUserPass.setText(profile.getSshAuthData().replaceAll(".", "*"));
        }
    }

    private void attachListeners() {
        rbModeHttp.setOnCheckedChangeListener(this);
        rbModeHttps.setOnCheckedChangeListener(this);
        swDirectMode.setOnCheckedChangeListener(this);
        swPayloadAfterTls.setOnCheckedChangeListener(this);

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    // -----------------------------------------------------------------------
    // Dynamic visibility
    // -----------------------------------------------------------------------

    private void updateDynamicVisibility() {
        if (rbModeHttp.isChecked()) {
            layoutSni.setVisibility(View.GONE);
            swPayloadAfterTls.setVisibility(View.GONE);
            swDirectMode.setVisibility(View.VISIBLE);
            layoutPayload.setVisibility(View.VISIBLE);
            if (swDirectMode.isChecked()) {
                layoutProxyServer.setVisibility(View.GONE);
            } else {
                layoutProxyServer.setVisibility(View.VISIBLE);
            }
        } else {
            layoutSni.setVisibility(View.VISIBLE);
            swPayloadAfterTls.setVisibility(View.VISIBLE);
            swDirectMode.setVisibility(View.GONE);
            layoutProxyServer.setVisibility(View.GONE);
            if (swPayloadAfterTls.isChecked()) {
                layoutPayload.setVisibility(View.VISIBLE);
            } else {
                layoutPayload.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateDynamicVisibility();
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    private void saveProfile() {
        String name = etProfileName.getText() != null
                ? etProfileName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            etProfileName.setError(getString(R.string.error_profile_name_empty));
            etProfileName.requestFocus();
            return;
        }

        if (!profile.isCustomFileLocked()) {
            String sshServer = etSshServer.getText() != null
                    ? etSshServer.getText().toString().trim() : "";
            if (TextUtils.isEmpty(sshServer)) {
                etSshServer.setError(getString(R.string.error_ssh_server_empty));
                etSshServer.requestFocus();
                return;
            }
        }

        profile.setName(name);

        if (!profile.isCustomFileLocked()) {
            profile.setSshServerDomain(getText(etSshServer));
            profile.setProxyIpDomain(getText(etProxyServer));
            profile.setSni(getText(etSni));
            profile.setPayloadKey(getText(etPayload));
            profile.setConnectionMode(rbModeHttps.isChecked() ? "MODO_HTTPS" : "MODO_HTTP");
            profile.setHttpDirect(swDirectMode.isChecked());
            profile.setPayloadAfterTls(swPayloadAfterTls.isChecked());
        }

        if (!profile.isCustomFileLocked() || !profile.isLockLoginEdit()) {
            profile.setSshAuthData(getText(etUserPass));
        }

        if (isNewProfile) {
            profileManager.addProfile(profile);
        } else {
            profileManager.updateProfile(profile);
        }

        Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
