package com.synapse.social.studioasinc.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.model.User;

import java.util.List;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder> {

    private final Context context;
    private final List<User> userList;
    private final OnUserClickListener onUserClickListener;

    public SearchUserAdapter(Context context, List<User> userList, OnUserClickListener onUserClickListener) {
        this.context = context;
        this.userList = userList;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public SearchUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_mention, parent, false);
        return new SearchUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchUserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class SearchUserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView profileImageView;
        private final TextView usernameTextView;
        private final TextView nicknameTextView;

        public SearchUserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profile_image);
            usernameTextView = itemView.findViewById(R.id.username_text);
            nicknameTextView = itemView.findViewById(R.id.nickname_text);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onUserClickListener.onUserClick(userList.get(position));
                }
            });
        }

        void bind(User user) {
            usernameTextView.setText(user.getUsername());
            nicknameTextView.setText(user.getNickname());

            if (user.getAvatar() != null && !user.getAvatar().equals("null")) {
                Glide.with(context).load(Uri.parse(user.getAvatar())).into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.avatar);
            }
        }
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }
}
