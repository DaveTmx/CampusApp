package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;

import com.example.campusapp.R;

/** Shows and shares only aggregate group labels and counts. */
public class LecturerSummaryActivity extends AppCompatActivity {

    public static final String EXTRA_SUMMARY = "com.example.campusapp.EXTRA_SUMMARY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_summary);

        String summary = getIntent().getStringExtra(EXTRA_SUMMARY);
        if (summary == null) {
            summary = "";
        }
        final String text = summary;

        ((TextView) findViewById(R.id.tvSummaryPreview)).setText(text);
        findViewById(R.id.btnShareSummary).setOnClickListener(v ->
                ShareCompat.IntentBuilder.from(this)
                        .setType("text/plain")
                        .setSubject(getString(R.string.summary_share_subject))
                        .setText(text)
                        .setChooserTitle(getString(R.string.summary_share_chooser))
                        .startChooser());
    }
}
