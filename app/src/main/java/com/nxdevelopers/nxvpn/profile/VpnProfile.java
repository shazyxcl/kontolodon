package com.nxdevelopers.nxvpn.profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Model representing a single VPN connection profile.
 * All configuration fields previously spread across SharedPreferences
 * are encapsulated here so each profile is self-contained.
 */
public class VpnProfile {

    private String id;
    private String name;

    private String connectionMode;   // "MODO_HTTP" | "MODO_HTTPS"
    private String sshServerDomain;  // host:port
    private String proxyIpDomain;    // host:port (HTTP mode without direct)
    private String sni;
    private String payloadKey;
    private String sshAuthData;      // user@password
    private boolean httpDirect;
    private boolean payloadAfterTls;

    private boolean customFileLocked;
    private boolean lockSettings;
    private boolean lockLoginEdit;
    private boolean onlyMobileData;
    private long    validadeMillis;
    private String  configMsg;

    public VpnProfile() {
        id              = UUID.randomUUID().toString();
        name            = "New Profile";
        connectionMode  = "MODO_HTTP";
        httpDirect      = true;
        payloadAfterTls = false;
        sshServerDomain = "";
        proxyIpDomain   = "";
        sni             = "";
        payloadKey      = "";
        sshAuthData     = "";
        configMsg       = "";
    }

    // --- Serialization ---

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id",              id);
        o.put("name",            name);
        o.put("connectionMode",  connectionMode);
        o.put("sshServerDomain", sshServerDomain);
        o.put("proxyIpDomain",   proxyIpDomain);
        o.put("sni",             sni);
        o.put("payloadKey",      payloadKey);
        o.put("sshAuthData",     sshAuthData);
        o.put("httpDirect",      httpDirect);
        o.put("payloadAfterTls", payloadAfterTls);
        o.put("customFileLocked",customFileLocked);
        o.put("lockSettings",    lockSettings);
        o.put("lockLoginEdit",   lockLoginEdit);
        o.put("onlyMobileData",  onlyMobileData);
        o.put("validadeMillis",  validadeMillis);
        o.put("configMsg",       configMsg);
        return o;
    }

    public static VpnProfile fromJson(JSONObject o) throws JSONException {
        VpnProfile p = new VpnProfile();
        p.id              = o.optString("id",              UUID.randomUUID().toString());
        p.name            = o.optString("name",            "Profile");
        p.connectionMode  = o.optString("connectionMode",  "MODO_HTTP");
        p.sshServerDomain = o.optString("sshServerDomain", "");
        p.proxyIpDomain   = o.optString("proxyIpDomain",   "");
        p.sni             = o.optString("sni",             "");
        p.payloadKey      = o.optString("payloadKey",      "");
        p.sshAuthData     = o.optString("sshAuthData",     "");
        p.httpDirect      = o.optBoolean("httpDirect",     false);
        p.payloadAfterTls = o.optBoolean("payloadAfterTls",false);
        p.customFileLocked= o.optBoolean("customFileLocked",false);
        p.lockSettings    = o.optBoolean("lockSettings",   false);
        p.lockLoginEdit   = o.optBoolean("lockLoginEdit",  false);
        p.onlyMobileData  = o.optBoolean("onlyMobileData", false);
        p.validadeMillis  = o.optLong("validadeMillis",    0);
        p.configMsg       = o.optString("configMsg",       "");
        return p;
    }

    // --- Getters & Setters ---

    public String getId()                         { return id; }

    public String getName()                       { return name; }
    public void   setName(String n)               { this.name = n; }

    public String getConnectionMode()             { return connectionMode; }
    public void   setConnectionMode(String m)     { this.connectionMode = m; }

    public String getSshServerDomain()            { return sshServerDomain; }
    public void   setSshServerDomain(String v)    { this.sshServerDomain = v; }

    public String getProxyIpDomain()              { return proxyIpDomain; }
    public void   setProxyIpDomain(String v)      { this.proxyIpDomain = v; }

    public String getSni()                        { return sni; }
    public void   setSni(String v)                { this.sni = v; }

    public String getPayloadKey()                 { return payloadKey; }
    public void   setPayloadKey(String v)         { this.payloadKey = v; }

    public String getSshAuthData()                { return sshAuthData; }
    public void   setSshAuthData(String v)        { this.sshAuthData = v; }

    public boolean isHttpDirect()                 { return httpDirect; }
    public void    setHttpDirect(boolean v)       { this.httpDirect = v; }

    public boolean isPayloadAfterTls()            { return payloadAfterTls; }
    public void    setPayloadAfterTls(boolean v)  { this.payloadAfterTls = v; }

    public boolean isCustomFileLocked()           { return customFileLocked; }
    public void    setCustomFileLocked(boolean v) { this.customFileLocked = v; }

    public boolean isLockSettings()               { return lockSettings; }
    public void    setLockSettings(boolean v)     { this.lockSettings = v; }

    public boolean isLockLoginEdit()              { return lockLoginEdit; }
    public void    setLockLoginEdit(boolean v)    { this.lockLoginEdit = v; }

    public boolean isOnlyMobileData()             { return onlyMobileData; }
    public void    setOnlyMobileData(boolean v)   { this.onlyMobileData = v; }

    public long    getValidadeMillis()            { return validadeMillis; }
    public void    setValidadeMillis(long v)      { this.validadeMillis = v; }

    public String  getConfigMsg()                 { return configMsg; }
    public void    setConfigMsg(String v)         { this.configMsg = v; }

    public String getDisplayName() {
        return (name == null || name.isEmpty()) ? "Unnamed Profile" : name;
    }
}
