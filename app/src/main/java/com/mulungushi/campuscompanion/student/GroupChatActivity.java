package com.mulungushi.campuscompanion.student;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.data.AppData;
import com.mulungushi.campuscompanion.data.ChatMessage;
import com.mulungushi.campuscompanion.data.Student;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Group chat for the logged-in student's own group. Every member of that
 * group sees the same message list (matched by Student.group), so it's
 * effectively a small update board scoped to G01/G02/G03/etc.
 */
public class GroupChatActivity extends AppCompatActivity {

    private AppData appData;
    private LinearLayout messagesContainer;
    private ScrollView chatScroll;
    private String groupName;
    private String myName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        appData = AppData.getInstance(this);
        Student me = appData.findStudentById(AppData.CURRENT_STUDENT_ID);
        groupName = me != null ? me.group : "";
        myName = me != null ? me.name : "You";

        ((TextView) findViewById(R.id.tvChatTitle)).setText("Group " + groupName + " Chat");
        ((TextView) findViewById(R.id.tvChatSubtitle)).setText("Updates between your group members");

        messagesContainer = findViewById(R.id.messagesContainer);
        chatScroll = findViewById(R.id.chatScroll);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText etMessage = findViewById(R.id.etMessage);
        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            appData.addChatMessage(groupName, myName, text);
            etMessage.setText("");
            renderMessages();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderMessages();
    }

    private void renderMessages() {
        messagesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        List<ChatMessage> groupMessages = appData.getMessagesForGroup(groupName);
        for (ChatMessage message : groupMessages) {
            View row = inflater.inflate(R.layout.item_chat_message, messagesContainer, false);
            boolean isMine = message.senderName.equals(myName);

            TextView tvSender = row.findViewById(R.id.tvSender);
            TextView tvText = row.findViewById(R.id.tvMessageText);
            TextView tvTime = row.findViewById(R.id.tvTime);

            tvSender.setText(isMine ? "You" : message.senderName);
            tvText.setText(message.text);
            tvTime.setText(timeFormat.format(new Date(message.timestamp)));

            if (isMine) {
                tvText.setBackgroundResource(R.drawable.bg_button_blue);
                tvText.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                tvText.setBackgroundResource(R.drawable.bg_card);
                tvText.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            }

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) row.getLayoutParams();
            params.gravity = isMine ? Gravity.END : Gravity.START;
            row.setLayoutParams(params);

            messagesContainer.addView(row);
        }

        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}
