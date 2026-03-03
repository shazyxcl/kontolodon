package com.nxdevelopers.nxvpn.settings;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.nxdevelopers.nxvpn.util.SharedPref;
import com.nxdevelopers.nxvpn.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final List<ResolveInfo> resolveInfoList;
    private final PackageManager    packageManager;
    private       Set<String>       selectedApps;
    private final SharedPref        sharedPref;

    public AppListAdapter(List<ResolveInfo> resolveInfoList,
                          PackageManager packageManager,
                          Context context) {
        this.resolveInfoList = resolveInfoList;
        this.packageManager  = packageManager;
        this.sharedPref      = SharedPref.getInstance(context);

        // Ambil set app yang sudah dipilih dari SharedPreferences
        selectedApps = sharedPref.getStringSet("selectedApps", new HashSet<>());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResolveInfo resolveInfo = resolveInfoList.get(position);

        // Load ikon app di background thread agar tidak blocking UI
        new Thread(() -> {
            Drawable drawable = resolveInfo.loadIcon(packageManager);
            holder.itemView.post(() -> holder.appIcon.setImageDrawable(drawable));
        }).start();

        holder.appTitle.setText(resolveInfo.loadLabel(packageManager));
        holder.appPackageName.setText(resolveInfo.activityInfo.packageName);

        // Set state switch berdasarkan selectedApps — tanpa trigger listener
        holder.mSwitch.setOnCheckedChangeListener(null);
        holder.mSwitch.setChecked(
                selectedApps.contains(resolveInfo.activityInfo.packageName));

        // Listener MaterialSwitch (M3)
        holder.mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String pkg = resolveInfo.activityInfo.packageName;
            if (isChecked) {
                selectedApps.add(pkg);
            } else {
                selectedApps.remove(pkg);
                sharedPref.removeAppFromList(pkg);
            }
            sharedPref.putStringSet("selectedApps", selectedApps);
            sharedPref.putBoolean(pkg, isChecked);
        });

        // Tap row juga toggle switch
        holder.itemView.setOnClickListener(v ->
                holder.mSwitch.setChecked(!holder.mSwitch.isChecked()));
    }

    @Override
    public int getItemCount() {
        return resolveInfoList.size();
    }

    public void selectAll() {
        for (ResolveInfo resolveInfo : resolveInfoList) {
            String pkg = resolveInfo.activityInfo.packageName;
            selectedApps.add(pkg);
            sharedPref.putBoolean(pkg, true);
        }
        sharedPref.putStringSet("selectedApps", selectedApps);
    }

    public void deselectAll() {
        for (ResolveInfo resolveInfo : resolveInfoList) {
            String pkg = resolveInfo.activityInfo.packageName;
            selectedApps.remove(pkg);
            sharedPref.putBoolean(pkg, false);
        }
        sharedPref.putStringSet("selectedApps", selectedApps);
    }

    // -----------------------------------------------------------------------
    // ViewHolder
    // -----------------------------------------------------------------------

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    appIcon;
        TextView     appTitle;
        TextView     appPackageName;
        MaterialSwitch mSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon        = itemView.findViewById(R.id.appIcon);
            appTitle       = itemView.findViewById(R.id.appName);
            appPackageName = itemView.findViewById(R.id.packageName);
            mSwitch        = itemView.findViewById(R.id.switchBtn);
        }
    }
}