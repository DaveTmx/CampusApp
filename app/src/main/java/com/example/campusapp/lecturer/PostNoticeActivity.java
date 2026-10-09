package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusapp.R;
import com.example.campusapp.data.AppData;

/** The lecturer's "post an update" screen — creates a new Notice visible to students. */
public class PostNoticeActivity extends AppCompatActivity {

    private String selectedCategory = "Academic";
    private TextView catAcademic, catAdmin, catUrgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_notice);

        AppData appData = AppData.getInstance(this);

        EditText etTitle = findViewById(R.id.etTitle);
        EditText etBody = findViewById(R.id.etBody);

        catAcademic = findViewById(R.id.catAcademic);
        catAdmin = findViewById(R.id.catAdmin);
        catUrgent = findViewById(R.id.catUrgent);

        catAcademic.setOnClickListener(v -> selectCategory("Academic"));
        catAdmin.setOnClickListener(v -> selectCategory("Admin"));
        catUrgent.setOnClickListener(v -> selectCategory("Urgent"));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnPost).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String body = etBody.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(body)) {
                Toast.makeText(this, "Please fill in a title and details", Toast.LENGTH_SHORT).show();
                return;
            }

            appData.addNotice(title, body, selectedCategory);
            Toast.makeText(this, "Notice posted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        highlight(catAcademic, category.equals("Academic"));
        highlight(catAdmin, category.equals("Admin"));
        highlight(catUrgent, category.equals("Urgent"));
    }

    private void highlight(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_pill_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_button_outline);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        }
    }
}
