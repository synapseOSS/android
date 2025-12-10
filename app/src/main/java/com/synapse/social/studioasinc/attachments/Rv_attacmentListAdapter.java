package com.synapse.social.studioasinc.attachments;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.synapse.social.studioasinc.AsyncUploadService;
import com.synapse.social.studioasinc.FileUtil;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.UploadFiles;

import java.util.ArrayList;
import java.util.HashMap;

public class Rv_attacmentListAdapter extends RecyclerView.Adapter<Rv_attacmentListAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> data;
    private final Context context;
    private final View attachmentLayoutListHolder;

    public Rv_attacmentListAdapter(@NonNull Context context,
                                   @NonNull ArrayList<HashMap<String, Object>> data,
                                   @NonNull View attachmentLayoutListHolder) {
        this.context = context;
        this.data = data;
        this.attachmentLayoutListHolder = attachmentLayoutListHolder;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_attactment, parent, false);
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(dpToPx(100), dpToPx(100));
        view.setLayoutParams(layoutParams);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View itemView = holder.itemView;

        final androidx.cardview.widget.CardView cardMediaItem = itemView.findViewById(R.id.cardMediaItem);
        final RelativeLayout imageWrapperRL = itemView.findViewById(R.id.imageWrapperRL);
        final ImageView previewIV = itemView.findViewById(R.id.previewIV);
        final LinearLayout overlayLL = itemView.findViewById(R.id.overlayLL);
        final com.google.android.material.progressindicator.CircularProgressIndicator uploadProgressCPI = itemView.findViewById(R.id.uploadProgressCPI);
        final ImageView closeIV = itemView.findViewById(R.id.closeIV);

        if (position < 0 || position >= data.size()) {
            Log.w("RvAttachment", "Invalid position in attachment adapter: " + position);
            itemView.setVisibility(View.GONE);
            return;
        }

        HashMap<String, Object> itemData = data.get(position);
        if (itemData == null) {
            Log.w("RvAttachment", "Null item data at position: " + position);
            itemView.setVisibility(View.GONE);
            return;
        }

        if (!itemData.containsKey("localPath") || itemData.get("localPath") == null) {
            itemView.setVisibility(View.GONE);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        itemView.setVisibility(View.VISIBLE);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(dpToPx(100), dpToPx(100)));

        String localPath = itemData.get("localPath").toString();
        try {
            previewIV.setImageDrawable(null);
            previewIV.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(localPath, 1024, 1024));
        } catch (Exception e) {
            Log.e("RvAttachment", "Error loading image preview: " + e.getMessage());
            previewIV.setImageResource(R.drawable.ph_imgbluredsqure);
        }

        String uploadState = itemData.getOrDefault("uploadState", "pending").toString();
        int progress = 0;
        if (itemData.containsKey("uploadProgress")) {
            try {
                progress = (int) Double.parseDouble(itemData.get("uploadProgress").toString());
            } catch (NumberFormatException e) {
                Log.w("RvAttachment", "Invalid upload progress value: " + itemData.get("uploadProgress"));
                progress = 0;
            }
        }

        switch (uploadState) {
            case "uploading":
                overlayLL.setVisibility(View.VISIBLE);
                overlayLL.setBackgroundColor(0x80000000);
                uploadProgressCPI.setVisibility(View.VISIBLE);
                uploadProgressCPI.setProgress(progress);
                closeIV.setVisibility(View.GONE);
                break;
            case "success":
                overlayLL.setVisibility(View.GONE);
                uploadProgressCPI.setVisibility(View.GONE);
                closeIV.setVisibility(View.VISIBLE);
                break;
            case "failed":
                overlayLL.setVisibility(View.VISIBLE);
                overlayLL.setBackgroundColor(0x80D32F2F);
                uploadProgressCPI.setVisibility(View.GONE);
                closeIV.setVisibility(View.VISIBLE);
                break;
            default:
                overlayLL.setVisibility(View.GONE);
                uploadProgressCPI.setVisibility(View.GONE);
                closeIV.setVisibility(View.VISIBLE);
                break;
        }

        closeIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= data.size()) {
                    Log.w("RvAttachment", "Invalid position for removal: " + adapterPosition);
                    return;
                }

                HashMap<String, Object> currentItemData = data.get(adapterPosition);
                if (currentItemData == null) {
                    Log.w("RvAttachment", "Null item data for removal at position: " + adapterPosition);
                    return;
                }

                if ("uploading".equals(currentItemData.get("uploadState"))) {
                    String localPath = String.valueOf(currentItemData.get("localPath"));
                    AsyncUploadService.cancelUpload(context, localPath);
                }

                data.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, data.size() - adapterPosition);

                if (currentItemData.containsKey("publicId")) {
                    String publicId = String.valueOf(currentItemData.get("publicId"));
                    if (publicId != null && !publicId.isEmpty()) {
                        UploadFiles.deleteByPublicId(publicId, new UploadFiles.DeleteCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("RvAttachment", "Successfully deleted attachment: " + publicId);
                            }
                            @Override
                            public void onFailure(String error) {
                                Log.e("RvAttachment", "Failed to delete attachment: " + error);
                            }
                        });
                    }
                }

                if (data.isEmpty()) {
                    attachmentLayoutListHolder.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View v) {
            super(v);
        }
    }

    private int dpToPx(int dp) {
        try {
            if (context.getResources() != null && context.getResources().getDisplayMetrics() != null) {
                return (int) (dp * context.getResources().getDisplayMetrics().density);
            }
        } catch (Exception e) {
            Log.e("RvAttachment", "Error converting dp to px: " + e.getMessage());
        }
        return dp;
    }
}

