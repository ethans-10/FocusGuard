package com.ethan.focusguard;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 55;

    private TextView statusText;
    private TextView scheduleText;
    private TextView liveStatusText;
    private TextView scheduleBadge;
    private EditText minutesInput;
    private EditText weatherCityInput;
    private TextView weatherStatusText;
    private TextView musicStatusText;
    private CheckBox scheduleEnabled;
    private int scheduleStart;
    private int scheduleEnd;
    private final Handler handler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scheduleStart = FocusManager.getScheduleStart(this);
        scheduleEnd = FocusManager.getScheduleEnd(this);
        FocusNotificationHelper.prepare(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FocusManager.recordUserActivity(this);
        FocusNotificationHelper.update(this);
        handler.post(refreshRunnable);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        FocusManager.recordUserActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 28), UiKit.dp(this, 20), UiKit.dp(this, 28));
        scrollView.addView(root);
        UiKit.page(this, scrollView);

        LinearLayout hero = UiKit.card(this);
        hero.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface_soft), UiKit.color(this, R.color.accent), 1, 28));
        root.addView(hero);

        TextView appLabel = UiKit.text(this, "FocusGuard", 15, UiKit.color(this, R.color.accent_text), true);
        appLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(appLabel);

        TextView title = UiKit.text(this, "Ready to focus?", 30, UiKit.color(this, R.color.text_primary), true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 6));
        hero.addView(title);

        statusText = UiKit.text(this, "", 18, UiKit.color(this, R.color.text_primary), true);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 10), UiKit.dp(this, 14), UiKit.dp(this, 10));
        statusText.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface), UiKit.color(this, R.color.border), 1, 18));
        hero.addView(statusText);
        UiKit.topMargin(this, statusText, 10);

        root.addView(simpleTimerCard());
        root.addView(simpleActionsCard());
        root.addView(simpleScheduleCard());
        root.addView(simpleStandbyCard());
        root.addView(simpleSetupCard());

        setContentView(scrollView);
        refreshStatus();
    }

    private LinearLayout simpleTimerCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Start a focus session", ""));

        Button quick30 = UiKit.button(this, "30 minutes");
        quick30.setOnClickListener(v -> {
            FocusManager.startTimer(this, 30);
            toast("30 minute focus started");
            refreshStatus();
        });
        card.addView(quick30);

        Button quick50 = UiKit.secondaryButton(this, "50 minutes");
        quick50.setOnClickListener(v -> {
            FocusManager.startTimer(this, 50);
            toast("50 minute focus started");
            refreshStatus();
        });
        card.addView(quick50);

        minutesInput = new EditText(this);
        minutesInput.setHint("Custom minutes");
        minutesInput.setTextSize(16);
        minutesInput.setSingleLine(true);
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 12));
        minutesInput.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface), UiKit.color(this, R.color.border), 1, 16));
        card.addView(minutesInput);
        UiKit.topMargin(this, minutesInput, 10);

        Button customTimer = UiKit.button(this, "Start custom");
        customTimer.setOnClickListener(v -> startCustomTimer());
        card.addView(customTimer);

        Button stopTimer = UiKit.dangerButton(this, "Stop focus");
        stopTimer.setOnClickListener(v -> {
            FocusManager.stopTimer(this);
            toast("Timer stopped");
            refreshStatus();
        });
        card.addView(stopTimer);
        return card;
    }

    private LinearLayout simpleActionsCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Main actions", ""));

        Button standby = UiKit.button(this, "Open standby clock");
        standby.setOnClickListener(v -> startActivity(new Intent(this, StandbyClockActivity.class)));
        card.addView(standby);

        Button chooseApps = UiKit.secondaryButton(this, "Choose blocked apps");
        chooseApps.setOnClickListener(v -> startActivity(new Intent(this, AppsActivity.class)));
        card.addView(chooseApps);

        Button notifications = UiKit.secondaryButton(this, "Notification settings");
        notifications.setOnClickListener(v -> requestOrOpenNotificationSettings());
        card.addView(notifications);
        return card;
    }

    private LinearLayout simpleScheduleCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Daily schedule", ""));

        scheduleEnabled = new CheckBox(this);
        scheduleEnabled.setText("Use daily schedule");
        scheduleEnabled.setTextSize(16);
        scheduleEnabled.setTextColor(UiKit.color(this, R.color.text_primary));
        scheduleEnabled.setChecked(FocusManager.isScheduleEnabled(this));
        card.addView(scheduleEnabled);

        scheduleBadge = UiKit.text(this, "", 15, UiKit.color(this, R.color.accent_text), true);
        scheduleBadge.setGravity(Gravity.CENTER);
        scheduleBadge.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 9), UiKit.dp(this, 12), UiKit.dp(this, 9));
        scheduleBadge.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface_soft), UiKit.color(this, R.color.accent), 1, 18));
        card.addView(scheduleBadge);
        UiKit.topMargin(this, scheduleBadge, 8);

        Button startButton = UiKit.secondaryButton(this, "Start time");
        startButton.setOnClickListener(v -> pickTime(true));
        card.addView(startButton);

        Button endButton = UiKit.secondaryButton(this, "End time");
        endButton.setOnClickListener(v -> pickTime(false));
        card.addView(endButton);

        Button saveSchedule = UiKit.button(this, "Save schedule");
        saveSchedule.setOnClickListener(v -> {
            FocusManager.setDailySchedule(this, scheduleEnabled.isChecked(), scheduleStart, scheduleEnd);
            toast("Schedule saved");
            refreshStatus();
        });
        card.addView(saveSchedule);
        return card;
    }

    private LinearLayout simpleStandbyCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Standby", ""));

        CheckBox autoCharge = new CheckBox(this);
        autoCharge.setText("Open when charging");
        autoCharge.setTextSize(16);
        autoCharge.setTextColor(UiKit.color(this, R.color.text_primary));
        autoCharge.setChecked(FocusManager.isAutoStandbyOnCharge(this));
        autoCharge.setOnCheckedChangeListener((buttonView, isChecked) -> FocusManager.setAutoStandbyOnCharge(this, isChecked));
        card.addView(autoCharge);

        CheckBox autoIdle = new CheckBox(this);
        autoIdle.setText("Open when idle");
        autoIdle.setTextSize(16);
        autoIdle.setTextColor(UiKit.color(this, R.color.text_primary));
        autoIdle.setChecked(FocusManager.isAutoStandbyOnIdle(this));
        autoIdle.setOnCheckedChangeListener((buttonView, isChecked) -> FocusManager.setAutoStandbyOnIdle(this, isChecked));
        card.addView(autoIdle);

        weatherCityInput = new EditText(this);
        weatherCityInput.setHint("Weather city");
        weatherCityInput.setText(FocusManager.getWeatherCity(this));
        weatherCityInput.setTextSize(16);
        weatherCityInput.setSingleLine(true);
        weatherCityInput.setInputType(InputType.TYPE_CLASS_TEXT);
        weatherCityInput.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 12));
        weatherCityInput.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface), UiKit.color(this, R.color.border), 1, 16));
        card.addView(weatherCityInput);
        UiKit.topMargin(this, weatherCityInput, 10);

        Button saveWeather = UiKit.secondaryButton(this, "Save weather city");
        saveWeather.setOnClickListener(v -> {
            FocusManager.setWeatherCity(this, weatherCityInput.getText().toString());
            WeatherHelper.refresh(this, success -> runOnUiThread(() -> toast(success ? "Weather updated" : "Weather could not update")));
        });
        card.addView(saveWeather);

        Button musicAccess = UiKit.secondaryButton(this, "Music access");
        musicAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        card.addView(musicAccess);
        return card;
    }

    private LinearLayout simpleSetupCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Setup", ""));

        Button accessibility = UiKit.button(this, "Accessibility settings");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        card.addView(accessibility);

        Button appSettings = UiKit.secondaryButton(this, "App settings");
        appSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        card.addView(appSettings);
        return card;
    }

    private LinearLayout timerCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Focus timer", "Start a quick session or set your own time"));

        Button quick30 = UiKit.button(this, "Start 30 minute focus");
        quick30.setOnClickListener(v -> {
            FocusManager.startTimer(this, 30);
            toast("30 minute focus started");
            refreshStatus();
        });
        card.addView(quick30);

        Button quick50 = UiKit.secondaryButton(this, "Start 50 minute deep work");
        quick50.setOnClickListener(v -> {
            FocusManager.startTimer(this, 50);
            toast("50 minute focus started");
            refreshStatus();
        });
        card.addView(quick50);

        minutesInput = new EditText(this);
        minutesInput.setHint("Custom minutes, e.g. 45");
        minutesInput.setTextSize(16);
        minutesInput.setSingleLine(true);
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 12));
        minutesInput.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface), UiKit.color(this, R.color.border), 1, 16));
        card.addView(minutesInput);
        UiKit.topMargin(this, minutesInput, 10);

        Button customTimer = UiKit.button(this, "Start custom timer");
        customTimer.setOnClickListener(v -> startCustomTimer());
        card.addView(customTimer);

        Button stopTimer = UiKit.dangerButton(this, "Stop current timer");
        stopTimer.setOnClickListener(v -> {
            FocusManager.stopTimer(this);
            toast("Timer stopped");
            refreshStatus();
        });
        card.addView(stopTimer);
        return card;
    }

    private LinearLayout scheduleCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Daily scheduled focus", "Useful for revision blocks after school or in the evening"));

        scheduleEnabled = new CheckBox(this);
        scheduleEnabled.setText("Use daily schedule");
        scheduleEnabled.setTextSize(17);
        scheduleEnabled.setTextColor(UiKit.color(this, R.color.text_primary));
        scheduleEnabled.setChecked(FocusManager.isScheduleEnabled(this));
        card.addView(scheduleEnabled);

        scheduleBadge = UiKit.text(this, "", 16, UiKit.color(this, R.color.accent_text), true);
        scheduleBadge.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 10), UiKit.dp(this, 14), UiKit.dp(this, 10));
        scheduleBadge.setGravity(Gravity.CENTER);
        scheduleBadge.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface_soft), UiKit.color(this, R.color.accent), 1, 18));
        card.addView(scheduleBadge);
        UiKit.topMargin(this, scheduleBadge, 8);

        scheduleText = UiKit.text(this, "", 15, UiKit.color(this, R.color.text_secondary), false);
        scheduleText.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 10));
        card.addView(scheduleText);

        Button startButton = UiKit.secondaryButton(this, "Choose start time");
        startButton.setOnClickListener(v -> pickTime(true));
        card.addView(startButton);

        Button endButton = UiKit.secondaryButton(this, "Choose end time");
        endButton.setOnClickListener(v -> pickTime(false));
        card.addView(endButton);

        Button saveSchedule = UiKit.button(this, "Save schedule");
        saveSchedule.setOnClickListener(v -> {
            FocusManager.setDailySchedule(this, scheduleEnabled.isChecked(), scheduleStart, scheduleEnd);
            toast("Schedule saved");
            refreshStatus();
        });
        card.addView(saveSchedule);
        return card;
    }

    private LinearLayout blockingCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Blocked apps", "Pick the apps FocusGuard should block during focus mode"));

        TextView note = UiKit.text(this, "The block screen appears only when your timer or schedule is active.", 15, UiKit.color(this, R.color.text_secondary), false);
        note.setPadding(0, 0, 0, UiKit.dp(this, 8));
        card.addView(note);

        Button chooseApps = UiKit.button(this, "Choose apps to block");
        chooseApps.setOnClickListener(v -> startActivity(new Intent(this, AppsActivity.class)));
        card.addView(chooseApps);
        return card;
    }

    private LinearLayout liveCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Now Bar and live notification", "Samsung decides if a third-party app is shown in Now Bar"));

        liveStatusText = UiKit.text(this, "", 15, UiKit.color(this, R.color.text_primary), true);
        liveStatusText.setPadding(0, 0, 0, UiKit.dp(this, 10));
        card.addView(liveStatusText);

        TextView liveInfo = UiKit.text(this,
                "FocusGuard now sends a proper ongoing countdown notification. It can show in the status bar and lock screen, but Now Bar only appears on supported One UI versions/settings.",
                15,
                UiKit.color(this, R.color.text_secondary),
                false);
        liveInfo.setPadding(0, 0, 0, UiKit.dp(this, 10));
        card.addView(liveInfo);

        Button notifications = UiKit.button(this, "Allow / manage notifications");
        notifications.setOnClickListener(v -> requestOrOpenNotificationSettings());
        card.addView(notifications);

        Button liveSettings = UiKit.secondaryButton(this, "Open live notification settings");
        liveSettings.setOnClickListener(v -> startActivity(FocusNotificationHelper.liveNotificationSettingsIntent(this)));
        card.addView(liveSettings);

        Button refreshLive = UiKit.secondaryButton(this, "Refresh live focus notification");
        refreshLive.setOnClickListener(v -> {
            FocusNotificationHelper.update(this);
            toast("Live notification refreshed");
            refreshStatus();
        });
        card.addView(refreshLive);
        return card;
    }


    private LinearLayout standbyExtrasCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Standby extras", "Battery, weather and currently playing music on the standby clock"));

        TextView info = UiKit.text(this,
                "Battery percentage works automatically. Weather uses the city below. Music uses Android notification access so it can read the media notification title from apps like Spotify.",
                15, UiKit.color(this, R.color.text_secondary), false);
        info.setPadding(0, 0, 0, UiKit.dp(this, 10));
        card.addView(info);

        weatherStatusText = UiKit.text(this, FocusManager.getWeatherSummary(this), 15, UiKit.color(this, R.color.text_primary), true);
        weatherStatusText.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 10), UiKit.dp(this, 14), UiKit.dp(this, 10));
        weatherStatusText.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface_soft), UiKit.color(this, R.color.accent), 1, 18));
        card.addView(weatherStatusText);

        weatherCityInput = new EditText(this);
        weatherCityInput.setHint("Weather city, e.g. London");
        weatherCityInput.setText(FocusManager.getWeatherCity(this));
        weatherCityInput.setTextSize(16);
        weatherCityInput.setSingleLine(true);
        weatherCityInput.setInputType(InputType.TYPE_CLASS_TEXT);
        weatherCityInput.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 12));
        weatherCityInput.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface), UiKit.color(this, R.color.border), 1, 16));
        card.addView(weatherCityInput);
        UiKit.topMargin(this, weatherCityInput, 10);

        Button saveWeather = UiKit.button(this, "Save and refresh weather");
        saveWeather.setOnClickListener(v -> {
            FocusManager.setWeatherCity(this, weatherCityInput.getText().toString());
            weatherStatusText.setText("Refreshing weather...");
            WeatherHelper.refresh(this, success -> runOnUiThread(() -> {
                weatherStatusText.setText(FocusManager.getWeatherSummary(this));
                toast(success ? "Weather updated" : "Weather could not update");
            }));
        });
        card.addView(saveWeather);

        musicStatusText = UiKit.text(this, FocusManager.getNowPlayingSummary(this), 15, UiKit.color(this, R.color.text_primary), true);
        musicStatusText.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 10), UiKit.dp(this, 14), UiKit.dp(this, 10));
        musicStatusText.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface_soft), UiKit.color(this, R.color.accent), 1, 18));
        card.addView(musicStatusText);
        UiKit.topMargin(this, musicStatusText, 10);

        Button musicAccess = UiKit.secondaryButton(this, "Enable music notification access");
        musicAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        card.addView(musicAccess);

        TextView note = UiKit.text(this,
                "Music will appear after you enable notification access and start playing something. It only uses the media notification title/artist; it does not read your messages.",
                14, UiKit.color(this, R.color.text_secondary), false);
        note.setPadding(0, UiKit.dp(this, 8), 0, 0);
        card.addView(note);
        return card;
    }

    private LinearLayout clockCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Standby clock", "Visual style previews, exact colour wheel and auto standby"));

        TextView standbyInfo = UiKit.text(this,
                "The clock now has a separate portrait and landscape layout, visual style cards, exact colour wheel, battery percentage, weather and currently playing music. Auto standby exits when you touch the screen.",
                15, UiKit.color(this, R.color.text_secondary), false);
        standbyInfo.setPadding(0, 0, 0, UiKit.dp(this, 10));
        card.addView(standbyInfo);

        CheckBox autoCharge = new CheckBox(this);
        autoCharge.setText("Auto-open standby when charger connects");
        autoCharge.setTextSize(16);
        autoCharge.setTextColor(UiKit.color(this, R.color.text_primary));
        autoCharge.setChecked(FocusManager.isAutoStandbyOnCharge(this));
        autoCharge.setOnCheckedChangeListener((buttonView, isChecked) -> FocusManager.setAutoStandbyOnCharge(this, isChecked));
        card.addView(autoCharge);

        CheckBox autoIdle = new CheckBox(this);
        autoIdle.setText("Auto-open after not using the phone");
        autoIdle.setTextSize(16);
        autoIdle.setTextColor(UiKit.color(this, R.color.text_primary));
        autoIdle.setChecked(FocusManager.isAutoStandbyOnIdle(this));
        autoIdle.setOnCheckedChangeListener((buttonView, isChecked) -> FocusManager.setAutoStandbyOnIdle(this, isChecked));
        card.addView(autoIdle);

        EditText idleMinutes = new EditText(this);
        idleMinutes.setHint("Idle minutes, e.g. 3");
        idleMinutes.setText(String.valueOf(FocusManager.getIdleMinutes(this)));
        idleMinutes.setTextSize(16);
        idleMinutes.setSingleLine(true);
        idleMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        idleMinutes.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 12));
        idleMinutes.setBackground(UiKit.roundedStroke(this, UiKit.color(this, R.color.surface), UiKit.color(this, R.color.border), 1, 16));
        card.addView(idleMinutes);
        UiKit.topMargin(this, idleMinutes, 10);

        Button saveAuto = UiKit.secondaryButton(this, "Save auto standby settings");
        saveAuto.setOnClickListener(v -> {
            try {
                int mins = Integer.parseInt(idleMinutes.getText().toString().trim());
                FocusManager.setIdleMinutes(this, mins);
                FocusManager.setAutoStandbyOnCharge(this, autoCharge.isChecked());
                FocusManager.setAutoStandbyOnIdle(this, autoIdle.isChecked());
                FocusManager.recordUserActivity(this);
                toast("Auto standby saved");
            } catch (NumberFormatException e) {
                toast("Use a whole number of minutes");
            }
        });
        card.addView(saveAuto);

        Button openClock = UiKit.button(this, "Open standby clock");
        openClock.setOnClickListener(v -> {
            Intent intent = new Intent(this, StandbyClockActivity.class);
            intent.putExtra("autoStandby", false);
            startActivity(intent);
        });
        card.addView(openClock);

        TextView autoNote = UiKit.text(this,
                "Auto idle needs the FocusGuard Accessibility service to be enabled. Some Android/Samsung versions may block automatic opening from the background; the manual clock still works.",
                14, UiKit.color(this, R.color.text_secondary), false);
        autoNote.setPadding(0, UiKit.dp(this, 8), 0, 0);
        card.addView(autoNote);
        return card;
    }

    private LinearLayout setupCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.sectionHeader(this, "Required setup", "Accessibility is needed so FocusGuard knows when a blocked app opens"));

        TextView setupInfo = UiKit.text(this, "Android makes you enable this manually. FocusGuard does not read typed text, messages or passwords.", 15, UiKit.color(this, R.color.text_secondary), false);
        setupInfo.setPadding(0, 0, 0, UiKit.dp(this, 10));
        card.addView(setupInfo);

        Button accessibility = UiKit.button(this, "Open Accessibility Settings");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        card.addView(accessibility);

        Button appSettings = UiKit.secondaryButton(this, "Open FocusGuard App Settings");
        appSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        card.addView(appSettings);
        return card;
    }

    private void requestOrOpenNotificationSettings() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        startActivity(FocusNotificationHelper.appNotificationSettingsIntent(this));
    }

    private void startCustomTimer() {
        String raw = minutesInput.getText().toString().trim();
        if (raw.isEmpty()) {
            toast("Enter minutes first");
            return;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            toast("Use a whole number of minutes");
            return;
        }
        if (minutes < 1 || minutes > 600) {
            toast("Choose between 1 and 600 minutes");
            return;
        }
        FocusManager.startTimer(this, minutes);
        toast(minutes + " minute focus started");
        refreshStatus();
    }

    private void pickTime(boolean pickingStart) {
        int current = pickingStart ? scheduleStart : scheduleEnd;
        int hour = current / 60;
        int minute = current % 60;
        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            int chosen = h * 60 + m;
            if (pickingStart) {
                scheduleStart = chosen;
            } else {
                scheduleEnd = chosen;
            }
            updateScheduleText();
        }, hour, minute, true);
        dialog.show();
    }

    private void refreshStatus() {
        boolean active = FocusManager.isFocusActive(this);
        statusText.setText(FocusManager.statusText(this));
        statusText.setTextColor(active ? UiKit.color(this, R.color.success) : UiKit.color(this, R.color.text_primary));

        if (liveStatusText != null) {
            String permission = FocusNotificationHelper.hasNotificationPermission(this)
                    ? "Notifications: allowed"
                    : "Notifications: permission needed";
            liveStatusText.setText(permission + "\n" + FocusNotificationHelper.liveCapabilityText(this));
        }
        if (weatherStatusText != null) weatherStatusText.setText(FocusManager.getWeatherSummary(this));
        if (musicStatusText != null) musicStatusText.setText(FocusManager.getNowPlayingSummary(this));
        updateScheduleText();
    }

    private void updateScheduleText() {
        if (scheduleText != null) {
            scheduleText.setText(String.format(Locale.UK, "Current schedule: %s to %s",
                    FocusManager.formatMinute(scheduleStart), FocusManager.formatMinute(scheduleEnd)));
        }
        if (scheduleBadge != null) {
            String enabled = scheduleEnabled != null && scheduleEnabled.isChecked() ? "Schedule selected" : "Schedule off";
            scheduleBadge.setText(enabled + "  •  " + FocusManager.formatMinute(scheduleStart) + " → " + FocusManager.formatMinute(scheduleEnd));
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
