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

package com.synapse.social.studioasinc.util;

import com.synapse.social.studioasinc.model.Attachment;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class for converting between HashMap-based attachments and type-safe Attachment objects.
 * Provides migration path from legacy HashMap format to new Parcelable format.
 */
public class AttachmentUtils {

    /**
     * Converts a HashMap representation to an Attachment object.
     * 
     * @param attachmentMap The HashMap containing attachment data
     * @return Attachment object or null if conversion fails
     */
    public static Attachment fromHashMap(HashMap<String, Object> attachmentMap) {
        if (attachmentMap == null) {
            return null;
        }
        
        try {
            Attachment attachment = new Attachment();
            
            // Extract publicId
            Object publicIdObj = attachmentMap.get("publicId");
            if (publicIdObj != null) {
                attachment.setPublicId(String.valueOf(publicIdObj));
            }
            
            // Extract URL
            Object urlObj = attachmentMap.get("url");
            if (urlObj != null) {
                attachment.setUrl(String.valueOf(urlObj));
            }
            
            // Extract dimensions
            Object widthObj = attachmentMap.get("width");
            if (widthObj instanceof Number) {
                attachment.setWidth(((Number) widthObj).intValue());
            }
            
            Object heightObj = attachmentMap.get("height");
            if (heightObj instanceof Number) {
                attachment.setHeight(((Number) heightObj).intValue());
            }
            
            // Extract type
            Object typeObj = attachmentMap.get("type");
            if (typeObj != null) {
                attachment.setType(String.valueOf(typeObj));
            } else {
                // Infer type from publicId if not explicitly set
                String publicId = attachment.getPublicId();
                if (publicId != null && publicId.contains("|video")) {
                    attachment.setType("video");
                } else {
                    attachment.setType("image");
                }
            }
            
            // Extract mimeType
            Object mimeTypeObj = attachmentMap.get("mimeType");
            if (mimeTypeObj != null) {
                attachment.setMimeType(String.valueOf(mimeTypeObj));
            }
            
            // Extract size
            Object sizeObj = attachmentMap.get("size");
            if (sizeObj instanceof Number) {
                attachment.setSize(((Number) sizeObj).longValue());
            }
            
            return attachment;
            
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("AttachmentUtils", "Error converting HashMap to Attachment: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Converts an Attachment object to a HashMap representation.
     * Used for backward compatibility with existing code.
     * 
     * @param attachment The Attachment object
     * @return HashMap representation
     */
    public static HashMap<String, Object> toHashMap(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        
        HashMap<String, Object> map = new HashMap<>();
        
        if (attachment.getPublicId() != null) {
            map.put("publicId", attachment.getPublicId());
        }
        
        if (attachment.getUrl() != null) {
            map.put("url", attachment.getUrl());
        }
        
        map.put("width", attachment.getWidth());
        map.put("height", attachment.getHeight());
        
        if (attachment.getType() != null) {
            map.put("type", attachment.getType());
        }
        
        if (attachment.getMimeType() != null) {
            map.put("mimeType", attachment.getMimeType());
        }
        
        map.put("size", attachment.getSize());
        
        return map;
    }
    
    /**
     * Converts a list of HashMap attachments to a list of Attachment objects.
     * 
     * @param attachmentMaps List of HashMap representations
     * @return List of Attachment objects
     */
    public static ArrayList<Attachment> fromHashMapList(ArrayList<HashMap<String, Object>> attachmentMaps) {
        if (attachmentMaps == null) {
            return new ArrayList<>();
        }
        
        ArrayList<Attachment> attachments = new ArrayList<>();
        
        for (HashMap<String, Object> map : attachmentMaps) {
            Attachment attachment = fromHashMap(map);
            if (attachment != null) {
                attachments.add(attachment);
            }
        }
        
        return attachments;
    }
    
    /**
     * Converts a list of Attachment objects to a list of HashMap representations.
     * 
     * @param attachments List of Attachment objects
     * @return List of HashMap representations
     */
    public static ArrayList<HashMap<String, Object>> toHashMapList(ArrayList<Attachment> attachments) {
        if (attachments == null) {
            return new ArrayList<>();
        }
        
        ArrayList<HashMap<String, Object>> maps = new ArrayList<>();
        
        for (Attachment attachment : attachments) {
            HashMap<String, Object> map = toHashMap(attachment);
            if (map != null) {
                maps.add(map);
            }
        }
        
        return maps;
    }
}