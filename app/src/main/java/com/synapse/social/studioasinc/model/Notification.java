package com.synapse.social.studioasinc.model;

public class Notification {
    private String from;
    private String message;
    private String type;
    private String postId;
    private String commentId;
    private long timestamp;

    public Notification() {
    }

    public Notification(String from, String message, String type, String postId, String commentId, long timestamp) {
        this.from = from;
        this.message = message;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.timestamp = timestamp;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
