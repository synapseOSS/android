/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data class representing a message attachment.
 * Implements Parcelable for efficient and type-safe data transfer between activities.
 */
public class Attachment implements Parcelable {
    
    private String publicId;
    private String url;
    private int width;
    private int height;
    private String type; // "image" or "video"
    private String mimeType;
    private long size;
    
    public Attachment() {
        // Default constructor
    }
    
    public Attachment(String publicId, String url, int width, int height, String type) {
        this.publicId = publicId;
        this.url = url;
        this.width = width;
        this.height = height;
        this.type = type;
    }
    
    // Parcelable implementation
    protected Attachment(Parcel in) {
        publicId = in.readString();
        url = in.readString();
        width = in.readInt();
        height = in.readInt();
        type = in.readString();
        mimeType = in.readString();
        size = in.readLong();
    }
    
    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }
        
        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(publicId);
        dest.writeString(url);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(type);
        dest.writeString(mimeType);
        dest.writeLong(size);
    }
    
    // Getters and setters
    public String getPublicId() {
        return publicId;
    }
    
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    /**
     * Checks if this attachment is a video based on the publicId containing "|video"
     * or the type being "video".
     * 
     * @return true if this is a video attachment
     */
    public boolean isVideo() {
        return "video".equals(type) || (publicId != null && publicId.contains("|video"));
    }
    
    /**
     * Checks if this attachment is an image.
     * 
     * @return true if this is an image attachment
     */
    public boolean isImage() {
        return "image".equals(type) || !isVideo();
    }
    
    /**
     * Gets the aspect ratio of the attachment.
     * 
     * @return the aspect ratio (width/height), or 1.0 if dimensions are invalid
     */
    public float getAspectRatio() {
        if (height > 0 && width > 0) {
            return (float) width / height;
        }
        return 1.0f;
    }
    
    /**
     * Checks if the attachment has a portrait orientation (taller than wide).
     * 
     * @return true if height > width
     */
    public boolean isPortrait() {
        return height > width && width > 0;
    }
    
    /**
     * Checks if the attachment has a landscape orientation (wider than tall).
     * 
     * @return true if width > height
     */
    public boolean isLandscape() {
        return width > height && height > 0;
    }
    
    @Override
    public String toString() {
        return "Attachment{" +
                "publicId='" + publicId + '\'' +
                ", url='" + url + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", type='" + type + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", size=" + size +
                '}';
    }
}