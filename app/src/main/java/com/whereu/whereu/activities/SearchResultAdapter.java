package com.whereu.whereu.activities;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;
import com.bumptech.glide.Glide;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    private List<SearchResult> searchResults;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onActionButtonClick(SearchResult result, int position);
        void onItemClick(SearchResult result);
    }

    public SearchResultAdapter(List<SearchResult> searchResults, OnItemClickListener listener) {
        this.searchResults = searchResults;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResult result = searchResults.get(position);
        holder.displayName.setText(result.getDisplayName());

        // Load profile image if available
        if (holder.profileImage != null) {
            Glide.with(holder.itemView)
                    .load(result.getProfilePhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(holder.profileImage);
        }

        if (result.isExistingUser()) {
            // Check if request status has changed
            if (result.getPreviousRequestStatus() != null && !result.getRequestStatus().equals(result.getPreviousRequestStatus())) {
                // Apply subtle animation
                if (!"approved".equals(result.getRequestStatus())) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(holder.actionButton, "alpha", 0f, 1f);
                    animator.setDuration(500); // 500ms duration
                    animator.start();
                }
            }

            switch (result.getRequestStatus()) {
                case "sent":
                case "pending":
                    // Show countdown timer if in cooldown period
                    if (result.isInCooldown()) {
                        long remainingTime = result.getCooldownRemainingTime();
                        holder.actionButton.setText("Wait " + SearchResult.formatCooldownTime(remainingTime));
                        holder.actionButton.setEnabled(false);
                    } else {
                        String suffix = formatRelativeOrAbsolute(result.getRequestSentTimestamp());
                        holder.actionButton.setText(suffix.isEmpty() ? "Request Sent" : ("Request Sent • " + suffix));
                        holder.actionButton.setEnabled(false);
                    }
                    break;
                case "approved":
                    holder.actionButton.setText("View Details");
                    holder.actionButton.setEnabled(true);
                    break;
                case "expired":
                    if (result.isInCooldown()) {
                        long remainingTime = result.getCooldownRemainingTime();
                        holder.actionButton.setText("Wait " + SearchResult.formatCooldownTime(remainingTime));
                        holder.actionButton.setEnabled(false);
                    } else {
                        holder.actionButton.setText("Request Again");
                        holder.actionButton.setEnabled(true);
                    }
                    break;
                case "rejected":
                    if (result.isInCooldown()) {
                        long remainingTime = result.getCooldownRemainingTime();
                        holder.actionButton.setText("Wait " + SearchResult.formatCooldownTime(remainingTime));
                        holder.actionButton.setEnabled(false);
                    } else {
                        holder.actionButton.setText("Request Again");
                        holder.actionButton.setEnabled(true);
                    }
                    break;
                default:
                    holder.actionButton.setText("Request Location");
                    holder.actionButton.setEnabled(true);
                    break;
            }
        } else {
            holder.actionButton.setText("Invite");
            holder.actionButton.setEnabled(true);
        }

        holder.actionButton.setOnClickListener(v -> listener.onActionButtonClick(result, position));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(result));
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView displayName;
        Button actionButton;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            displayName = itemView.findViewById(R.id.display_name);
            actionButton = itemView.findViewById(R.id.action_button);
        }
    }

    private String formatRelativeOrAbsolute(long ts) {
        if (ts <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - ts);

        long oneDayMs = 24L * 60L * 60L * 1000L;
        if (diff < oneDayMs) {
            long minutes = diff / (60L * 1000L);
            if (minutes < 60) {
                if (minutes <= 0) return "just now";
                return minutes + " min ago";
            } else {
                long hours = minutes / 60L;
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            }
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(ts));
    }

    public static class SearchResult implements Parcelable {
        private String displayName;
        private String userId;
        private String phoneNumber;
        private String email;
        private boolean isExistingUser;
        private boolean isCurrentUserEmail;
        private String profilePhotoUrl;
        private String requestStatus;
        private String requestId;
        private String previousRequestStatus; // Added field
        private long lastRequestTimestamp; // last approved/request time for frequent card
        private long requestSentTimestamp;
        private long cooldownRemainingTime; // timestamp when request was sent for cooldown

        public SearchResult(String displayName, String userId, String phoneNumber, String email, boolean isExistingUser, boolean isCurrentUserEmail, String profilePhotoUrl, String requestStatus) {
            this.displayName = displayName;
            this.userId = userId;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.isExistingUser = isExistingUser;
            this.isCurrentUserEmail = isCurrentUserEmail;
            this.profilePhotoUrl = profilePhotoUrl;
            this.requestStatus = requestStatus;
            this.previousRequestStatus = requestStatus; // Initialize previous status
            this.lastRequestTimestamp = 0L;
            this.requestSentTimestamp = 0L;
            this.cooldownRemainingTime = 0L;
        }

        protected SearchResult(Parcel in) {
            displayName = in.readString();
            userId = in.readString();
            phoneNumber = in.readString();
            email = in.readString();
            isExistingUser = in.readByte() != 0;
            isCurrentUserEmail = in.readByte() != 0;
            profilePhotoUrl = in.readString();
            requestStatus = in.readString();
            requestId = in.readString();
            previousRequestStatus = in.readString(); // Read from parcel
            lastRequestTimestamp = in.readLong();
            requestSentTimestamp = in.readLong();
            cooldownRemainingTime = in.readLong();
        }

        public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
            @Override
            public SearchResult createFromParcel(Parcel in) {
                return new SearchResult(in);
            }

            @Override
            public SearchResult[] newArray(int size) {
                return new SearchResult[size];
            }
        };

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getEmail() {
            return email;
        }

        public boolean isExistingUser() {
            return isExistingUser;
        }

        public void setExistingUser(boolean existingUser) {
            isExistingUser = existingUser;
        }

        public boolean isCurrentUserEmail() {
            return isCurrentUserEmail;
        }

        public String getProfilePhotoUrl() {
            return profilePhotoUrl;
        }

        public void setProfilePhotoUrl(String profilePhotoUrl) {
            this.profilePhotoUrl = profilePhotoUrl;
        }

        public String getRequestStatus() {
            return requestStatus;
        }

        public String getPreviousRequestStatus() {
            return previousRequestStatus;
        }

        public void setRequestStatus(String requestStatus) {
            if (this.requestStatus != null) {
                this.previousRequestStatus = this.requestStatus;
            }
            this.requestStatus = requestStatus;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public long getLastRequestTimestamp() {
            return lastRequestTimestamp;
        }

        public void setLastRequestTimestamp(long lastRequestTimestamp) {
            this.lastRequestTimestamp = lastRequestTimestamp;
        }

        public long getRequestSentTimestamp() {
            return requestSentTimestamp;
        }

        public void setRequestSentTimestamp(long requestSentTimestamp) {
            this.requestSentTimestamp = requestSentTimestamp;
        }

        public long getCooldownRemainingTime() {
            if (!isInCooldown()) return 0L;
            long cooldownMs = "rejected".equals(requestStatus) ? (60L * 60L * 1000L) : (60L * 1000L);
            long cooldownEndTime = requestSentTimestamp + cooldownMs;
            return cooldownEndTime - System.currentTimeMillis();
        }

        public void setCooldownRemainingTime(long cooldownRemainingTime) {
            this.cooldownRemainingTime = cooldownRemainingTime;
        }

        public boolean isInCooldown() {
            if (requestSentTimestamp == 0L) return false;
            long cooldownMs = "rejected".equals(requestStatus) ? (60L * 60L * 1000L) : (60L * 1000L);
            long cooldownEndTime = requestSentTimestamp + cooldownMs;
            return System.currentTimeMillis() < cooldownEndTime;
        }

        public static String formatCooldownTime(long milliseconds) {
            long seconds = Math.max(0, milliseconds / 1000);
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            long remainingSeconds = seconds % 60;
            if (hours > 0) {
                return hours + "h " + remainingMinutes + "m";
            }
            return remainingMinutes + "m " + remainingSeconds + "s";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(displayName);
            dest.writeString(userId);
            dest.writeString(phoneNumber);
            dest.writeString(email);
            dest.writeByte((byte) (isExistingUser ? 1 : 0));
            dest.writeByte((byte) (isCurrentUserEmail ? 1 : 0));
            dest.writeString(profilePhotoUrl);
            dest.writeString(requestStatus);
            dest.writeString(requestId);
            dest.writeString(previousRequestStatus); // Write to parcel
            dest.writeLong(lastRequestTimestamp);
            dest.writeLong(requestSentTimestamp);
            dest.writeLong(cooldownRemainingTime);
        }
    }
}
