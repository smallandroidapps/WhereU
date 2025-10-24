package com.whereu.whereu.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;

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

        if (result.isExistingUser()) {
            switch (result.getRequestStatus()) {
                case "pending":
                    holder.actionButton.setText("Request Sent");
                    holder.actionButton.setEnabled(false);
                    break;
                case "approved":
                    holder.actionButton.setText("View Details");
                    holder.actionButton.setEnabled(true);
                    break;
                case "expired":
                    holder.actionButton.setText("Request Again");
                    holder.actionButton.setEnabled(true);
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

        public SearchResult(String displayName, String userId, String phoneNumber, String email, boolean isExistingUser, boolean isCurrentUserEmail, String profilePhotoUrl, String requestStatus) {
            this.displayName = displayName;
            this.userId = userId;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.isExistingUser = isExistingUser;
            this.isCurrentUserEmail = isCurrentUserEmail;
            this.profilePhotoUrl = profilePhotoUrl;
            this.requestStatus = requestStatus;
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

        public void setRequestStatus(String requestStatus) {
            this.requestStatus = requestStatus;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
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
        }
    }
}
