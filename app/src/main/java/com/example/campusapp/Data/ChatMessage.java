package com.example.campusapp.data;

/** A single message in a group's chat, visible only to members of that group. */
public class ChatMessage {
    public long id;
    public String group;       // which group this message belongs to, e.g. "G02"
    public String senderName;
    public String text;
    public long timestamp;     // System.currentTimeMillis() when sent

    public ChatMessage(long id, String group, String senderName, String text, long timestamp) {
        this.id = id;
        this.group = group;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }
}
