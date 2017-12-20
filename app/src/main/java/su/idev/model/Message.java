package su.idev.model;

import android.content.Context;

import com.stfalcon.chatkit.commons.models.IMessage;

import java.util.Date;

public class Message implements IMessage  {

    private String id;
    private String text;
    private Date createdAt;
    private User user;
    private Context context;

    public Message(Context context, String id, User user, String text, Date createdAt) {
        this.context = context;
        this.id = id;
        this.text = text;
        this.user = user;
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getText() { return text; }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public User getUser() {
        return this.user;
    }

    public Context getContext() {
        return context;
    }

    public void setText(String text) {
        this.text = text;
    }





}
