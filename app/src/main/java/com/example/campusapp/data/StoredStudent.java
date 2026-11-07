package com.example.campusapp.data;

/**
 * A student as saved on this phone (AppData). The screens use model.Student instead. `id` is an internal identifier that never changes,
 * so a student can still be found (e.g. after a "number correction" is
 * approved) even though `number` — the field shown on screen — is editable.
 */
public class StoredStudent {
    public String id;
    public String number;
    public String name;
    public String group;
    public String program; // one of Programs.ALL

    public StoredStudent(String id, String number, String name, String group, String program) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.group = group;
        this.program = program;
    }
}
