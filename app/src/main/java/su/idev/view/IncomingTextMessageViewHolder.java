package su.idev.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.pitt.library.fresh.FreshDownloadView;
import com.stfalcon.chatkit.messages.MessageHolders;

import java.io.IOException;
import java.net.URL;

import su.idev.chatmap.ImageViewActivity;
import su.idev.chatmap.R;
import su.idev.model.Message;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by culibinl on 13.07.17.
 */

public class IncomingTextMessageViewHolder
        extends MessageHolders.IncomingTextMessageViewHolder<Message> {

    private TextView displayName;
    private TextView messageText;
    private FreshDownloadView freshDownloadView;


    public IncomingTextMessageViewHolder(View itemView) {
        super(itemView);
        displayName = (TextView)itemView.findViewById(R.id.displayName);
        messageText = (TextView)itemView.findViewById(R.id.messageText);
        freshDownloadView = (FreshDownloadView)itemView.findViewById(+R.id.pitt);
        freshDownloadView.setProgressColor(R.color.default_circular_color);
        freshDownloadView.setRadius(32f);
        freshDownloadView.setProgressTextSize(22f);
        freshDownloadView.setVisibility(View.GONE);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);
        displayName.setText(message.getUser().getName());
        ImageViewActivity.setTextViewHTML(message.getContext(), freshDownloadView, messageText, message.getText(), true);
    }

}
