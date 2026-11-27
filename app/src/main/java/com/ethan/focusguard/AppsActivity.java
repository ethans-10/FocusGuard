package com.ethan.focusguard;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AppsActivity extends Activity {
    private TextView countText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 34), UiKit.dp(this, 20), UiKit.dp(this, 34));
        scrollView.addView(root);
        UiKit.page(this, scrollView);

        TextView title = UiKit.text(this, "Choose blocked apps", 30, UiKit.color(this, R.color.text_primary), true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView note = UiKit.text(this, "Tick distracting apps. They are only blocked while a timer or daily schedule is active.", 16, UiKit.color(this, R.color.text_secondary), false);
        note.setGravity(Gravity.CENTER_HORIZONTAL);
        note.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 16));
        root.addView(note);

        countText = UiKit.text(this, "", 15, UiKit.color(this, R.color.accent_text), true);
        countText.setGravity(Gravity.CENTER);
        countText.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 10), UiKit.dp(this, 14), UiKit.dp(this, 10));
        countText.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface_soft), UiKit.color(this, R.color.accent), 1, 18));
        root.addView(countText);

        LinearLayout listCard = UiKit.card(this);
        root.addView(listCard);

        List<AppEntry> apps = loadLaunchableApps();
        Set<String> blocked = FocusManager.getBlockedPackages(this);
        updateCount(blocked.size());

        for (AppEntry app : apps) {
            CheckBox cb = new CheckBox(this);
            cb.setText(app.label + "\n" + app.packageName);
            cb.setTextSize(16);
            cb.setTextColor(UiKit.color(this, R.color.text_primary));
            cb.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 10));
            cb.setChecked(blocked.contains(app.packageName));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                FocusManager.setBlocked(this, app.packageName, isChecked);
                updateCount(FocusManager.getBlockedPackages(this).size());
            });
            listCard.addView(cb);
        }

        setContentView(scrollView);
    }

    private void updateCount(int count) {
        if (countText != null) {
            countText.setText(count + " apps selected");
        }
    }

    private List<AppEntry> loadLaunchableApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        List<AppEntry> entries = new ArrayList<>();

        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String label = info.loadLabel(pm).toString();
            entries.add(new AppEntry(label, pkg));
        }

        Collections.sort(entries, Comparator.comparing(a -> a.label.toLowerCase()));
        return entries;
    }

    private static class AppEntry {
        final String label;
        final String packageName;

        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }
}
