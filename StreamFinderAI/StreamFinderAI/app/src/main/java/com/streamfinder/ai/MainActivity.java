package com.streamfinder.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout results;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private int requestVersion = 0;
    private final int bg = Color.rgb(12, 14, 24);
    private final int panel = Color.rgb(36, 39, 56);
    private final int accent = Color.rgb(190, 165, 255);

    static class Title {
        int id;
        String name, type, year, overview;
        double rating;
        Title(int id, String name, String type, String year, String overview, double rating) {
            this.id=id; this.name=name; this.type=type; this.year=year; this.overview=overview; this.rating=rating;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 28, 36, 28);
        scroll.addView(root);
        TextView title = label("▶ StreamFinder AI", 30, accent);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(label("Discover movies and TV shows and where to stream them in the US", 17, Color.WHITE));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search movies or TV shows");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.LTGRAY);
        search.setBackground(shape(panel));
        search.setPadding(20, 18, 20, 18);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 24, 0, 18);
        root.addView(search, sp);
        status = label("", 15, Color.LTGRAY);
        root.addView(status);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);
        root.addView(label("Movie and TV data: TMDB. Streaming availability: JustWatch via TMDB. Not endorsed or certified by TMDB. Availability may change.", 12, Color.LTGRAY));
        setContentView(scroll);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
                String q = s.toString().trim();
                pendingSearch = () -> loadTitles(q);
                handler.postDelayed(pendingSearch, 450);
            }
            public void afterTextChanged(Editable e) {}
        });
        if (BuildConfig.TMDB_API_KEY.isEmpty()) status.setText("TMDB API key missing. Set the GitHub Actions secret TMDB_API_KEY and rebuild.");
        else loadTitles("");
    }

    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setPadding(12, 12, 12, 12);
        return v;
    }
    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(14); return d;
    }
    private JSONObject fetch(String path) throws Exception {
        String sep = path.contains("?") ? "&" : "?";
        URL url = new URL("https://api.themoviedb.org/3" + path + sep + "api_key=" + URLEncoder.encode(BuildConfig.TMDB_API_KEY, "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
        try {
            if (conn.getResponseCode() != 200) throw new Exception("TMDB HTTP " + conn.getResponseCode());
            try (InputStream input = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
                return new JSONObject(out.toString("UTF-8"));
            }
        } finally { conn.disconnect(); }
    }
    private void loadTitles(String query) {
        if (BuildConfig.TMDB_API_KEY.isEmpty()) return;
        final int version = ++requestVersion;
        status.setText("Loading real TMDB results...");
        results.removeAllViews();
        new Thread(() -> {
            try {
                String path = query.isEmpty() ? "/trending/all/week" : "/search/multi?query=" + URLEncoder.encode(query, "UTF-8");
                JSONArray array = fetch(path).optJSONArray("results");
                ArrayList<Title> found = new ArrayList<>();
                if (array != null) for (int i=0; i<array.length(); i++) {
                    JSONObject o = array.optJSONObject(i); if (o == null) continue;
                    String type = o.optString("media_type", "");
                    if (!type.equals("movie") && !type.equals("tv")) continue;
                    String name = o.optString(type.equals("tv") ? "name" : "title", "");
                    if (name.isEmpty()) continue;
                    String date = o.optString(type.equals("tv") ? "first_air_date" : "release_date", "");
                    String year = date.length() >= 4 ? date.substring(0,4) : "Unknown year";
                    found.add(new Title(o.optInt("id"), name, type, year, o.optString("overview", ""), o.optDouble("vote_average", 0)));
                }
                runOnUiThread(() -> {
                    if (version != requestVersion) return;
                    status.setText(found.isEmpty() ? "No titles found." : found.size() + " titles found");
                    for (Title t : found) addTitle(t);
                });
            } catch (Exception e) {
                runOnUiThread(() -> { if (version == requestVersion) status.setText("Could not load titles: " + e.getMessage()); });
            }
        }).start();
    }
    private void addTitle(Title t) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(18);
        b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        b.setText(t.name + " (" + t.year + ")\n" + (t.type.equals("tv") ? "TV show" : "Movie") + " · TMDB " + String.format(Locale.US,"%.1f/10",t.rating));
        b.setBackground(shape(panel)); b.setPadding(20,20,20,20);
        b.setFocusable(true); b.setOnClickListener(v -> showDetails(t));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,8,0,10);
        results.addView(b,p);
    }
    private void showDetails(Title t) {
        AlertDialog loading = new AlertDialog.Builder(this).setTitle(t.name).setMessage("Loading real streaming availability...").setNegativeButton("Close",null).create();
        loading.show();
        new Thread(() -> {
            String providers = "No US subscription streaming options listed.";
            String link = "";
            try {
                JSONObject response = fetch("/" + t.type + "/" + t.id + "/watch/providers");
                JSONObject us = response.optJSONObject("results");
                us = us == null ? null : us.optJSONObject("US");
                if (us != null) {
                    link = us.optString("link", "");
                    JSONArray list = us.optJSONArray("flatrate");
                    if (list != null && list.length() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int i=0; i<Math.min(3,list.length()); i++) {
                            JSONObject provider = list.optJSONObject(i);
                            if (provider != null) sb.append(i+1).append(". ").append(provider.optString("provider_name")).append("\n");
                        }
                        providers = sb.toString();
                    }
                }
            } catch (Exception e) { providers = "Provider lookup failed: " + e.getMessage(); }
            final String text = t.overview + "\n\nTMDB rating: " + String.format(Locale.US,"%.1f/10",t.rating) + "\n\nTop 3 US subscription streaming options (TMDB/JustWatch):\n" + providers + "\nProvider listings are not proof of an active subscription.";
            final String watch = link;
            runOnUiThread(() -> {
                loading.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(t.name).setMessage(text).setNegativeButton("Close",null);
                if (!watch.isEmpty()) builder.setPositiveButton("Where to watch", (d,w) -> {
                    try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(watch))); }
                    catch (Exception e) { Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show(); }
                });
                builder.show();
            });
        }).start();
    }
}
