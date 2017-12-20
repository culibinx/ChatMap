package su.idev.view;

import android.view.View;
import android.widget.TextView;

import com.pitt.library.fresh.FreshDownloadView;
import com.stfalcon.chatkit.messages.MessageHolders;

import su.idev.chatmap.ImageViewActivity;
import su.idev.chatmap.R;
import su.idev.model.Message;

/**
 * Created by culibinl on 13.07.17.
 */

public class OutcomingTextMessageViewHolder
        extends MessageHolders.OutcomingTextMessageViewHolder<Message> {

    private TextView messageText;
    private FreshDownloadView freshDownloadView;

    public OutcomingTextMessageViewHolder(View itemView) {
        super(itemView);
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
        ImageViewActivity.setTextViewHTML(message.getContext(), freshDownloadView, messageText, message.getText(), false);
    }
}
