package com.example.campusapp.util;

import com.example.campusapp.model.GroupCount;

import java.util.List;

/**
 * Builds the group summary spreadsheet (CSV). Group codes and counts only:
 * no names, student numbers or programmes, so the file is safe to share.
 */
public final class GroupCsv {

    private GroupCsv() {
    }

    public static String build(List<GroupCount> counts, int capacity) {
        StringBuilder csv = new StringBuilder("group,students,capacity,spaces_left\n");
        for (GroupCount c : counts) {
            int spacesLeft = Math.max(0, capacity - c.count);
            csv.append(c.group).append(',')
                    .append(c.count).append(',')
                    .append(capacity).append(',')
                    .append(spacesLeft).append('\n');
        }
        return csv.toString();
    }
}
