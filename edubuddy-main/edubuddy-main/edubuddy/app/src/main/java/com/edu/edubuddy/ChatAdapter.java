package com.edu.edubuddy;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Map;

public class ChatAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Map<String, String>> messages;
    private final LayoutInflater inflater;

    public ChatAdapter(Context context, ArrayList<Map<String, String>> messages) {
        this.context = context;
        this.messages = messages;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        Map<String, String> message = messages.get(i);
        String type = message.get("type");

        if ("study_request".equals(type)) {
            convertView = inflater.inflate(R.layout.chat_study_request_item, parent, false);
            TextView sender = convertView.findViewById(R.id.senderName);
            TextView text = convertView.findViewById(R.id.messageText);
            Button joinButton = convertView.findViewById(R.id.joinButton);
            CardView studyRequestLayout = convertView.findViewById(R.id.studyRequestLayout);

            sender.setText(message.get("sender"));
            text.setText(message.get("text"));

            // Automatically detect URLs and make them clickable
            Linkify.addLinks(text, Linkify.WEB_URLS);

            int[] superChatColors = {
                    Color.parseColor("#FF007F"),
                    Color.parseColor("#FF6700"),
                    Color.parseColor("#C11B17"),
                    Color.parseColor("#9D00FF")
            };
            int colorIndex = i % superChatColors.length;
            studyRequestLayout.setCardBackgroundColor(superChatColors[colorIndex]);

            joinButton.setVisibility(View.VISIBLE);
            joinButton.setOnClickListener(v -> showJoinDialog(message.get("topic")));

        } else if ("image".equals(type)) {
            convertView = inflater.inflate(R.layout.chat_image_item, parent, false);
            ImageView imageView = convertView.findViewById(R.id.chatImage);
            TextView sender = convertView.findViewById(R.id.senderName);

            sender.setText(message.get("sender"));
            Glide.with(context).load(message.get("imageUrl")).into(imageView);

            imageView.setOnClickListener(v -> showImageLightbox(message.get("imageUrl")));

        } else {
            convertView = inflater.inflate(R.layout.chat_text_item, parent, false);
            TextView sender = convertView.findViewById(R.id.senderName);
            TextView text = convertView.findViewById(R.id.messageText);

            sender.setText(message.get("sender"));
            text.setText(message.get("text"));

            // Automatically detect URLs and make them clickable
            Linkify.addLinks(text, Linkify.WEB_URLS);

            // Add a custom click listener to open URLs in a browser
            text.setOnClickListener(v -> {
                String url = text.getText().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                }
            });
        }

        return convertView;
    }

    private void showImageLightbox(String imageUrl) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_lightbox);
        dialog.setCanceledOnTouchOutside(true);

        ImageView fullImage = dialog.findViewById(R.id.dialogImageView);
        Glide.with(context).load(imageUrl).into(fullImage);

        dialog.show();
    }

    private void showJoinDialog(String topic) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_join_study_request);
        dialog.setCanceledOnTouchOutside(true);

        TextView topicText = dialog.findViewById(R.id.studyTopic);
        Button joinButton = dialog.findViewById(R.id.joinStudyButton);

        topicText.setText("Do you want to join the study session on " + topic + "?");

        joinButton.setOnClickListener(v -> {
            Toast.makeText(context, "You have joined the study session on " + topic, Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            // Optionally launch the group chat
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("groupName", topic);
            context.startActivity(intent);
        });

        dialog.show();
    }
}
