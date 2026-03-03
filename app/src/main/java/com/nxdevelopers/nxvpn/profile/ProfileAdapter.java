package com.nxdevelopers.nxvpn.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import com.nxdevelopers.nxvpn.R;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    public interface ProfileAdapterListener {
        void onProfileSelected(VpnProfile profile);
        void onEditProfile(VpnProfile profile);
        void onDeleteProfile(VpnProfile profile);
        void onExportProfile(VpnProfile profile);
    }

    private final List<VpnProfile>       profiles;
    private       String                 activeProfileId;
    private final ProfileAdapterListener listener;

    public ProfileAdapter(List<VpnProfile> profiles,
                          String activeProfileId,
                          ProfileAdapterListener listener) {
        this.profiles        = profiles;
        this.activeProfileId = activeProfileId;
        this.listener        = listener;
    }

    public void setActiveProfileId(String id) {
        this.activeProfileId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final VpnProfile profile = profiles.get(position);

        holder.tvProfileName.setText(profile.getDisplayName());

        // Reflect active selection without triggering listener loop
        holder.rbActive.setOnCheckedChangeListener(null);
        holder.rbActive.setChecked(profile.getId().equals(activeProfileId));
        holder.rbActive.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        listener.onProfileSelected(profile);
                    }
                });

        // Tapping the row also selects the profile
        holder.itemView.setOnClickListener(v -> listener.onProfileSelected(profile));

        // Overflow menu — Material You: menggunakan MaterialAlertDialogBuilder
        holder.btnMenu.setOnClickListener(v -> {
            Context ctx = v.getContext();
            String[] menuItems = {
                    ctx.getString(R.string.edit_profile),
                    ctx.getString(R.string.delete_profile),
                    ctx.getString(R.string.export_profile)
            };

            new MaterialAlertDialogBuilder(ctx)
                    .setTitle(profile.getDisplayName())
                    .setItems(menuItems, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                listener.onEditProfile(profile);
                                break;
                            case 1:
                                // Konfirmasi hapus dengan dialog M3
                                new MaterialAlertDialogBuilder(ctx)
                                        .setTitle(R.string.delete_profile_title)
                                        .setMessage(R.string.delete_profile_confirm)
                                        .setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.delete, (d, w) ->
                                                listener.onDeleteProfile(profile))
                                        .show();
                                break;
                            case 2:
                                listener.onExportProfile(profile);
                                break;
                        }
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton rbActive;
        TextView    tvProfileName;
        ImageButton btnMenu;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rbActive      = (RadioButton)  itemView.findViewById(R.id.rb_profile_active);
            tvProfileName = (TextView)     itemView.findViewById(R.id.tv_profile_name);
            btnMenu       = (ImageButton)  itemView.findViewById(R.id.btn_profile_menu);
        }
    }
}