package in.akhilesh.ember;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Store {
    final SharedPreferences prefs;
    Store(Context context) { prefs = context.getSharedPreferences("ember", Context.MODE_PRIVATE); }
    String get(String key, String fallback) { return prefs.getString(key, fallback); }
    void put(String key, String value) { prefs.edit().putString(key, value).apply(); }
    JSONArray sessions() {
        try { return new JSONArray(get("sessions", "[]")); } catch (JSONException e) { return new JSONArray(); }
    }
    JSONObject active() {
        try { return new JSONObject(get("active", "{}")); } catch (JSONException e) { return new JSONObject(); }
    }
    void start(String game, String profile, DeviceSnapshot d) {
        JSONObject o = new JSONObject();
        try {
            o.put("game", game).put("profile", profile).put("start", System.currentTimeMillis())
                .put("batteryStart", d.battery).put("temperatureStart", d.temperature == null ? JSONObject.NULL : d.temperature);
        } catch (JSONException ignored) { }
        prefs.edit().putString("active", o.toString()).commit();
    }
    void finish(DeviceSnapshot d, String rating, String note) {
        JSONObject o = active();
        if (!o.has("start")) return;
        try {
            o.put("end", System.currentTimeMillis()).put("batteryEnd", d.battery)
                .put("temperatureEnd", d.temperature == null ? JSONObject.NULL : d.temperature).put("rating", rating).put("note", note);
        } catch (JSONException ignored) { }
        JSONArray old = sessions(), updated = new JSONArray();
        updated.put(o);
        for (int i = 0; i < Math.min(old.length(), 199); i++) updated.put(old.optJSONObject(i));
        prefs.edit().putString("sessions", updated.toString()).remove("active").commit();
    }
    String exportCsv() {
        StringBuilder s = new StringBuilder("game,profile,start_epoch_ms,end_epoch_ms,duration_seconds,battery_start_percent,battery_end_percent,battery_temperature_start_c,battery_temperature_end_c,rating,note\n");
        JSONArray a = sessions();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i); if (o == null) continue;
            String[] values = {o.optString("game"), o.optString("profile"), "" + o.optLong("start"), "" + o.optLong("end"),
                "" + Metrics.duration(o.optLong("start"), o.optLong("end")) / 1000, "" + o.optInt("batteryStart", -1), "" + o.optInt("batteryEnd", -1),
                o.optString("temperatureStart", ""), o.optString("temperatureEnd", ""), o.optString("rating"), o.optString("note")};
            for (int j = 0; j < values.length; j++) { if (j > 0) s.append(',');
                // Prevent spreadsheet formula execution in user-entered notes.
                String value = values[j];
                value = Metrics.safeCell(value);
                s.append(Metrics.csv(value));
            }
            s.append('\n');
        }
        return s.toString();
    }
}
