package com.app.fourscontracting.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists failed/offline attendance punches and retries when network returns.
 */
public class OfflineAttendanceQueue {
    private static final String PREF = "attendance_offline_queue";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 50;

    private final SharedPreferences prefs;

    public OfflineAttendanceQueue(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public synchronized void enqueue(AttendancePayload payload) {
        if (payload == null) return;
        try {
            JSONArray arr = readArray();
            // De-dupe by client_request_id
            for (int i = 0; i < arr.length(); i++) {
                JSONObject existing = arr.optJSONObject(i);
                if (existing != null && payload.clientRequestId.equals(existing.optString("client_request_id"))) {
                    return;
                }
            }
            while (arr.length() >= MAX_ITEMS) {
                arr.remove(0);
            }
            arr.put(payload.toJson());
            prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized List<AttendancePayload> snapshot() {
        List<AttendancePayload> list = new ArrayList<>();
        JSONArray arr = readArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) {
                list.add(AttendancePayload.fromJson(o));
            }
        }
        return list;
    }

    public synchronized void remove(String clientRequestId) {
        if (clientRequestId == null) return;
        try {
            JSONArray arr = readArray();
            JSONArray next = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (clientRequestId.equals(o.optString("client_request_id"))) continue;
                next.put(o);
            }
            prefs.edit().putString(KEY_ITEMS, next.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized int size() {
        return readArray().length();
    }

    private JSONArray readArray() {
        try {
            String raw = prefs.getString(KEY_ITEMS, "[]");
            return new JSONArray(raw != null ? raw : "[]");
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
