package com.example.campusapp.lecturer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.repository.FakeStudentRosterRepository; // swap for RealStudentRepository later
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Student;
import com.example.campusapp.ui.lecturer.LecturerRosterViewModel;
import com.example.campusapp.ui.lecturer.LecturerRosterViewModelFactory;
import com.example.campusapp.ui.lecturer.RosterUiState;
import com.example.campusapp.util.AppExecutors;
import com.example.campusapp.viewmodel.EditorViewModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Paged, searchable lecturer roster driven by LecturerRosterViewModel.
 * The group chips filter the loaded rows on screen.
 */
public class LecturerRosterActivity extends AppCompatActivity {

    /** Chip value for students with no confirmed group. */
    private static final String UNASSIGNED = "UNASSIGNED";
    private static final String[] GROUPS = {"G01", "G02", "G03", "G04"};

    private LecturerRosterViewModel viewModel;
    private RosterAdapter adapter;
    private TextView statusText;
    private TextView cachedBanner;

    /** The last state from the ViewModel. */
    private RosterUiState lastState;
    /** null = "All". */
    private String groupFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_roster);

        viewModel = new ViewModelProvider(this,
                new LecturerRosterViewModelFactory(new FakeStudentRosterRepository(AppExecutors.get())))
                .get(LecturerRosterViewModel.class);

        TextInputEditText searchBox = findViewById(R.id.etSearch);
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
                    viewModel.onLoadNextPage();
                }
            }
        });

        // The ViewModel debounces, so every keystroke can be passed straight through.
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onQueryChanged(String.valueOf(s));
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        ChipGroup chips = findViewById(R.id.chipGroupGroups);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            groupFilter = checkedIds.isEmpty() ? null : groupForChip(checkedIds.get(0));
            render();
        });

        statusText.setOnClickListener(v -> {
            if (lastState != null && lastState.status == RosterUiState.Status.ERROR) {
                viewModel.retry();
            }
        });

        viewModel.getState().observe(this, state -> {
            lastState = state;
            render();
        });

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

    /** Wired from android:onClick in the layout. */
    public void onShareSummaryClicked(View v) {
        // Counts are only complete once every page has been loaded.
        if (lastState == null || lastState.status == RosterUiState.Status.LOADING || lastState.hasMore) {
            Toast.makeText(this, R.string.summary_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, LecturerSummaryActivity.class);
        intent.putExtra(LecturerSummaryActivity.EXTRA_SUMMARY,
                buildShareSummary(countGroups(lastState.students)));
        startActivity(intent);
    }

    private void render() {
        if (lastState == null) {
            return;
        }
        List<Student> visible = new ArrayList<>();
        for (Student s : lastState.students) {
            if (matchesGroup(s)) {
                visible.add(s);
            }
        }
        adapter.submitList(visible);
        cachedBanner.setVisibility(
                lastState.status == RosterUiState.Status.OFFLINE ? View.VISIBLE : View.GONE);

        RosterUiState.Status status = lastState.status;
        if (status == RosterUiState.Status.SUCCESS && visible.isEmpty()) {
            status = RosterUiState.Status.EMPTY; // the group filter hid every row
        }
        if (status == RosterUiState.Status.SUCCESS || status == RosterUiState.Status.OFFLINE) {
            statusText.setVisibility(View.GONE);
        } else {
            statusText.setText(messageFor(status));
            statusText.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesGroup(Student s) {
        if (groupFilter == null) {
            return true;
        }
        if (UNASSIGNED.equals(groupFilter)) {
            return s.confirmedGroup == null;
        }
        return groupFilter.equals(s.confirmedGroup);
    }

    private void openEditor(Student student) {
        Intent intent = new Intent(this, EditStudentActivity.class);
        intent.putExtra(EditorViewModel.EXTRA_STUDENT_ID, student.studentId);
        startActivity(intent);
    }

    private void confirmDelete(Student student) {
        // Delete belongs to the Architecture/Backend contract. The brief requires a confirm
        // dialog and cancel. Wire it to the repository's deleteStudent once that contract is settled.
        Toast.makeText(this, "Delete is not wired yet", Toast.LENGTH_SHORT).show();
    }

    private static int messageFor(RosterUiState.Status status) {
        switch (status) {
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
        if (chipId == R.id.chipUnassigned) return UNASSIGNED;
        return null; // "All"
    }

    private static List<GroupCount> countGroups(List<Student> students) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String g : GROUPS) {
            counts.put(g, 0);
        }
        for (Student s : students) {
            if (s.confirmedGroup != null && counts.containsKey(s.confirmedGroup)) {
                counts.put(s.confirmedGroup, counts.get(s.confirmedGroup) + 1);
            }
        }
        List<GroupCount> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            out.add(new GroupCount(e.getKey(), e.getValue()));
        }
        return out;
    }

    /** Only group labels and counts: no names or numbers leave the app. */
    private static String buildShareSummary(List<GroupCount> counts) {
        StringBuilder sb = new StringBuilder();
        for (GroupCount c : counts) {
            sb.append(c.group).append(": ").append(c.count).append('/').append(GroupCount.CAPACITY).append('\n');
        }
        return sb.toString().trim();
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
