package com.example.campusapp.student;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.Programs;
import com.example.campusapp.data.TimetableEntry;
import com.example.campusapp.util.CurrentProgram;
import com.example.campusapp.util.VenueIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Campus map: every venue on the student's own programme timetable, with the classes held
 * there, plus the main offices. Tapping a venue opens it in Google Maps. Opened from the
 * Schedule tab with EXTRA_VENUE, that venue is outlined and shown first.
 *
 * LecturerMapActivity reuses this screen with the venues of every programme.
 */
public class CampusMapActivity extends AppCompatActivity {

    /** Venue name to outline and show first, e.g. the room of a class tapped on the Schedule tab. */
    public static final String EXTRA_VENUE = "venue";

    /** Where Google Maps should search; added to every venue name. */
    private static final String CAMPUS = "Mulungushi University, Kabwe, Zambia";

    /**
     * Exact pins: venue name -> {latitude, longitude}. Venues listed here open at that point;
     * the rest open a Google Maps search for "venue, Mulungushi University". Add a venue by
     * long-pressing its building in Google Maps, copying the two numbers, and adding a line such as
     *   COORDINATES.put(Timetable.NEW_DINING, new double[]{-14.xxxx, 28.xxxx});
     */
    private static final Map<String, double[]> COORDINATES = new HashMap<>();

    /** Offices that are not on any timetable but students still look for. */
    private static final String[][] OFFICES = {
            {"Academic Registry", "Main Administration Block, 1st Floor"},
            {"Bursar's Office", "Main Administration Block, Ground Floor"},
            {"University Library", "West Campus Plaza"},
            {"Dean of Students", "Student Center, Room 12"},
    };

    private LinearLayout container;
    private List<VenueIndex.Venue> venues = new ArrayList<>();
    private String highlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_map);

        container = findViewById(R.id.venueContainer);
        highlight = getIntent().getStringExtra(EXTRA_VENUE);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.MAP, destinations());

        EditText search = findViewById(R.id.etVenueSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                render(s.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The programme (Profile tab) or the timetable (lecturer) may have changed.
        List<TimetableEntry> entries;
        if (allProgrammes()) {
            entries = new ArrayList<>();
            for (String code : Programs.CODES) {
                entries.addAll(AppData.getInstance(this).getClassesForProgram(code));
            }
            ((TextView) findViewById(R.id.tvMapSubtitle)).setText("Venues on every programme timetable");
        } else {
            String programme = CurrentProgram.code(this);
            entries = AppData.getInstance(this).getClassesForProgram(programme);
            ((TextView) findViewById(R.id.tvMapSubtitle)).setText(
                    "Where your " + Programs.toLabel(programme) + " classes are held");
        }
        venues = VenueIndex.build(entries, allProgrammes());
        render(((EditText) findViewById(R.id.etVenueSearch)).getText().toString());
    }

    /** Lecturers see every programme's venues; students only their own. */
    protected boolean allProgrammes() {
        return false;
    }

    protected Map<NavTab, Class<?>> destinations() {
        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, StudentHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, StudentScheduleActivity.class);
        destinations.put(NavTab.NOTICES, NoticesActivity.class);
        destinations.put(NavTab.PROFILE, StudentProfileActivity.class);
        return destinations;
    }

    private void render(String query) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // The tapped venue first, then the rest in timetable order.
        List<VenueIndex.Venue> ordered = new ArrayList<>();
        for (VenueIndex.Venue v : venues) {
            if (highlight != null && VenueIndex.sameVenue(v.name, highlight)) ordered.add(0, v);
            else ordered.add(v);
        }

        heading(allProgrammes() ? "CLASS VENUES" : "YOUR CLASS VENUES");
        int shown = 0;
        for (VenueIndex.Venue v : ordered) {
            if (!v.matches(query)) continue;
            boolean isHighlight = highlight != null && VenueIndex.sameVenue(v.name, highlight);
            addCard(inflater, v.name, TextUtils.join("\n", v.classes), isHighlight);
            shown++;
        }
        if (shown == 0) {
            note(venues.isEmpty() ? "No classes on the timetable yet." : "No venues match your search.");
        }

        heading("CAMPUS OFFICES");
        int offices = 0;
        for (String[] office : OFFICES) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !(office[0] + " " + office[1]).toLowerCase(Locale.ROOT).contains(q)) continue;
            addCard(inflater, office[0], office[1], false);
            offices++;
        }
        if (offices == 0) {
            note("No offices match your search.");
        }

        if (highlight != null && query.trim().isEmpty()) {
            ((ScrollView) findViewById(R.id.mapScroll)).post(() ->
                    ((ScrollView) findViewById(R.id.mapScroll)).scrollTo(0, 0));
        }
    }

    private void addCard(LayoutInflater inflater, String name, String details, boolean outlined) {
        View card = inflater.inflate(R.layout.item_venue, container, false);
        ((TextView) card.findViewById(R.id.tvVenueName)).setText(name);
        ((TextView) card.findViewById(R.id.tvVenueDetails)).setText(details);
        if (outlined) {
            card.setBackgroundResource(R.drawable.bg_card_highlight);
        }
        View.OnClickListener open = v -> openInMaps(name);
        card.setOnClickListener(open);
        card.findViewById(R.id.tvVenueDirections).setOnClickListener(open);
        container.addView(card);
    }

    private void heading(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        tv.setTextSize(11);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, pad, 0, pad);
        container.addView(tv);
    }

    private void note(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_gray_light));
        tv.setTextSize(13);
        int pad = (int) (6 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, 0, 0, pad * 2);
        container.addView(tv);
    }

    /** Opens the venue in Google Maps (or any maps app), falling back to the browser. */
    private void openInMaps(String venue) {
        double[] pin = null;
        for (Map.Entry<String, double[]> e : COORDINATES.entrySet()) {
            if (VenueIndex.sameVenue(e.getKey(), venue)) pin = e.getValue();
        }
        String label = venue + ", " + CAMPUS;
        Uri geo = pin != null
                ? Uri.parse("geo:" + pin[0] + "," + pin[1] + "?q=" + pin[0] + "," + pin[1]
                        + "(" + Uri.encode(venue) + ")")
                : Uri.parse("geo:0,0?q=" + Uri.encode(label));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (ActivityNotFoundException noMapsApp) {
            Uri web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(
                    pin != null ? pin[0] + "," + pin[1] : label));
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, web));
            } catch (ActivityNotFoundException noBrowser) {
                Toast.makeText(this, "No maps app or browser is installed.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
