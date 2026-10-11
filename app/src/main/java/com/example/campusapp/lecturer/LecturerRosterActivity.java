package com.example.campusapp.lecturer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.Data.repository.FakeStudentRepository; // swap for RealStudentRepository later
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Student;
import com.example.campusapp.ui.lecturer.LecturerRosterViewModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LecturerRosterActivity extends AppCompatActivity {

    private static final long SEARCH_DEBOUNCE_MS = 300;

    private LecturerRosterViewModel viewModel;
    private RosterAdapter adapter;
    private TextInputEditText searchBox;
    private TextView statusText;
    private TextView cachedBanner;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable applySearch =
            () -> viewModel.setQuery(String.valueOf(searchBox.getText()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_roster);

        viewModel = new ViewModelProvider(this,
                new LecturerRosterViewModel.Factory(new FakeStudentRepository()))
                .get(LecturerRosterViewModel.class);

        searchBox = findViewById(R.id.etSearch);
        statusText = findViewById(R.id.tvStatus);
        cachedBanner = findViewById(R.id.tvCachedBanner);
        RecyclerView list = findViewById(R.id.rvRoster);

        adapter = new RosterAdapter(this::openEditor, this::confirmDelete);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) {
                    return; // only when scrolling down
                }
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 5) {
                    viewModel.loadNextPageIfNeeded();
                }
            }
        });

        // Debounce: restart the timer on each keystroke, search once the typing pauses.
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(applySearch);
                handler.postDelayed(applySearch, SEARCH_DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        ChipGroup chips = findViewById(R.id.chipGroupGroups);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                viewModel.setFilters(null, groupForChip(checkedIds.get(0)));
            }
        });

        viewModel.getStudents().observe(this, adapter::submitList);
        viewModel.getUiState().observe(this, this::showState);
        viewModel.getFromCache().observe(this, cached ->
                cachedBanner.setVisibility(Boolean.TRUE.equals(cached) ? View.VISIBLE : View.GONE));
        viewModel.getGroupCounts().observe(this, counts -> { }); // keep the counts hot for Share

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, EditStudentActivity.class)));

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);
        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(applySearch);
        super.onDestroy();
    }

    /** Wired from android:onClick in the layout. */
    public void onShareSummaryClicked(View v) {
        List<GroupCount> counts = viewModel.getGroupCounts().getValue();
        if (counts == null) {
            Toast.makeText(this, R.string.summary_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, LecturerSummaryActivity.class);
        intent.putExtra(LecturerSummaryActivity.EXTRA_SUMMARY,
                LecturerRosterViewModel.buildShareSummary(counts));
        startActivity(intent);
    }

    private void openEditor(Student student) {
        Intent intent = new Intent(this, EditStudentActivity.class);
        intent.putExtra(EditStudentActivity.EXTRA_STUDENT_ID, student.studentId);
        startActivity(intent);
    }

    private void confirmDelete(Student student) {
        // Delete belongs to the Architecture/Backend contract. The brief requires a confirm
        // dialog and cancel. Wire it to the repository's deleteStudent once that contract is settled.
        Toast.makeText(this, "Delete is not wired yet", Toast.LENGTH_SHORT).show();
    }

    private void showState(LecturerRosterViewModel.UiState state) {
        if (state == LecturerRosterViewModel.UiState.CONTENT) {
            statusText.setVisibility(View.GONE);
            return;
        }
        statusText.setText(messageFor(state));
        statusText.setVisibility(View.VISIBLE);
    }

    private int messageFor(LecturerRosterViewModel.UiState state) {
        switch (state) {
            case EMPTY:           return R.string.roster_empty;
            case OFFLINE:         return R.string.roster_offline;
            case SESSION_EXPIRED: return R.string.roster_session_expired;
            case ERROR:           return R.string.roster_error;
            case LOADING:
            default:              return R.string.roster_loading;
        }
    }

    private static String groupForChip(int chipId) {
        if (chipId == R.id.chipG01) return "G01";
        if (chipId == R.id.chipG02) return "G02";
        if (chipId == R.id.chipG03) return "G03";
        if (chipId == R.id.chipG04) return "G04";
        if (chipId == R.id.chipUnassigned) return LecturerRosterViewModel.UNASSIGNED;
        return null; // "All"
    }

    // ----- adapter ------------------------------------------------------------

    interface StudentAction { void run(Student student); }

    static class RosterAdapter extends ListAdapter<Student, RosterAdapter.Holder> {

        private final StudentAction onEdit;
        private final StudentAction onDelete;

        RosterAdapter(StudentAction onEdit, StudentAction onDelete) {
            super(new DiffUtil.ItemCallback<Student>() {
                @Override
                public boolean areItemsTheSame(@NonNull Student a, @NonNull Student b) {
                    return Objects.equals(a.studentId, b.studentId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Student a, @NonNull Student b) {
                    return a.version == b.version
                            && Objects.equals(a.name, b.name)
                            && Objects.equals(a.confirmedGroup, b.confirmedGroup)
                            && Objects.equals(a.studentNumber, b.studentNumber);
                }
            });
            this.onEdit = onEdit;
            this.onDelete = onDelete;
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView details;
            final Button edit;
            final Button delete;

            Holder(View view) {
                super(view);
                name = view.findViewById(R.id.tvName);
                details = view.findViewById(R.id.tvDetails);
                edit = view.findViewById(R.id.btnEdit);
                delete = view.findViewById(R.id.btnDelete);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_roster_row, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Student s = getItem(position);
            Context ctx = h.itemView.getContext();
            String group = s.confirmedGroup == null ? "Unassigned" : s.confirmedGroup;

            h.name.setText(s.name);
            h.details.setText(ctx.getString(R.string.roster_row_details,
                    s.studentNumber, group, s.programme));
            h.edit.setContentDescription(ctx.getString(R.string.roster_edit_description, s.name));

            h.edit.setOnClickListener(v -> onEdit.run(s));
            h.delete.setOnClickListener(v -> onDelete.run(s));
        }
    }
}
