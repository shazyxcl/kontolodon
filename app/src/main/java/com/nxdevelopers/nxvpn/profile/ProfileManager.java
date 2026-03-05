package com.nxdevelopers.nxvpn.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {

    private static final String PREFS_NAME    = "PROFILE_PREFS";
    private static final String KEY_PROFILES  = "VPN_PROFILES";
    private static final String KEY_ACTIVE_ID = "ACTIVE_PROFILE_ID";

    private static ProfileManager instance;

    private final SharedPreferences prefs;
    private final List<VpnProfile>  profiles = new ArrayList<VpnProfile>();
    private String activeProfileId;

    private ProfileManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized ProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileManager(context);
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public List<VpnProfile> getProfiles() {
        return profiles;
    }

    public VpnProfile getActiveProfile() {
        if (activeProfileId == null) return null;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(activeProfileId)) {
                return profiles.get(i);
            }
        }
        return null;
    }

    public void setActiveProfile(String profileId) {
        activeProfileId = profileId;
        prefs.edit().putString(KEY_ACTIVE_ID, profileId).apply();
    }

    public void addProfile(VpnProfile profile) {
        profiles.add(profile);
        save();
    }

    public void updateProfile(VpnProfile updated) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(updated.getId())) {
                profiles.set(i, updated);
                save();
                return;
            }
        }
        addProfile(updated);
    }

    public void deleteProfile(String profileId) {
        // Manual removal instead of removeIf to avoid requiring API 24+
        for (int i = profiles.size() - 1; i >= 0; i--) {
            if (profiles.get(i).getId().equals(profileId)) {
                profiles.remove(i);
                break;
            }
        }
        if (profileId.equals(activeProfileId)) {
            activeProfileId = null;
            prefs.edit().remove(KEY_ACTIVE_ID).apply();
        }
        save();
    }

    public VpnProfile findById(String id) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(id)) {
                return profiles.get(i);
            }
        }
        return null;
    }

    public String exportProfileJson(String profileId) {
        VpnProfile p = findById(profileId);
        if (p == null) return null;
        try {
            return p.toJson().toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encodes the profile JSON as Base64 and returns the full
     * clipboard string in the format:  nxvpn://<base64>
     */
    public String exportProfileNxLink(String profileId) {
        String json = exportProfileJson(profileId);
        if (json == null) return null;
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP);
        return "nxvpn://" + b64;
    }

    /**
     * Writes the profile as a .nx file (Base64-encoded JSON) to the
     * URI provided by the SAF file-picker (ACTION_CREATE_DOCUMENT).
     * Returns true on success.
     */
    public boolean exportProfileToUri(Context context, String profileId, Uri uri) {
        String nxLink = exportProfileNxLink(profileId);
        if (nxLink == null) return false;
        try {
            OutputStream os = context.getContentResolver().openOutputStream(uri);
            if (os == null) return false;
            os.write(nxLink.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public VpnProfile importProfileJson(String json) {
        try {
            JSONObject obj     = new JSONObject(json);
            VpnProfile profile = VpnProfile.fromJson(obj);
            updateProfile(profile);
            return profile;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    private void load() {
        profiles.clear();
        activeProfileId = prefs.getString(KEY_ACTIVE_ID, null);

        String raw = prefs.getString(KEY_PROFILES, null);
        if (raw == null) return;

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                profiles.add(VpnProfile.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < profiles.size(); i++) {
            try {
                arr.put(profiles.get(i).toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply();
    }
}
