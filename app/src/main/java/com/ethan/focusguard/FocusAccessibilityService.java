
package com.ethan.focusguard;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class FocusAccessibilityService extends AccessibilityService {

    private static final String TAG = "FocusGuardCharging";
    private static final long CHARGE_DEBOUNCE_MS = 2500L;

    private long lastBlockLaunch = 0L;
    private String lastBlockedPackage = "";
    private final Handler handler = new Handler(Looper.getMainLooper());

    // null = charging state has not yet been established.
    private Boolean lastChargingState = null;
    private boolean chargingReceiverRegistered = false;

    // Shared across service instances within the same process.
    private static long lastChargeLaunchElapsed = 0L;

    private final BroadcastReceiver chargingReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;

                    String action = intent.getAction();

                    if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                        Log.i(TAG, "POWER_CONNECTED received");
                        onChargingStateObserved(true, "broadcast");
                    } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                        Log.i(TAG, "POWER_DISCONNECTED received");
                        onChargingStateObserved(false, "broadcast");
                    }
                }
            };

    private void registerChargingMonitor() {
        if (chargingReceiverRegistered) {
            Log.d(TAG, "Charging monitor already registered");
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                        chargingReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );
            } else {
                registerReceiver(chargingReceiver, filter);
            }

            chargingReceiverRegistered = true;
            Log.i(TAG, "Runtime charging receiver registered");

        } catch (RuntimeException e) {
            Log.e(TAG, "Receiver registration failed", e);
        }
    }

    private void unregisterChargingMonitor() {
        if (!chargingReceiverRegistered) return;

        try {
            unregisterReceiver(chargingReceiver);
            Log.i(TAG, "Runtime charging receiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver unregister failed", e);
        } finally {
            chargingReceiverRegistered = false;
        }
    }

    private void checkInitialChargingState() {
        try {
            Intent battery = registerReceiver(
                    null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );

            if (battery == null) {
                Log.w(TAG, "Initial battery state unavailable");
                return;
            }

            int plugged = battery.getIntExtra(
                    BatteryManager.EXTRA_PLUGGED, 0
            );

            boolean charging = plugged != 0;

            Log.i(TAG, "Initial charging state: "
                    + charging + ", plugged=" + plugged);

            onChargingStateObserved(charging, "initial");

        } catch (RuntimeException e) {
            Log.e(TAG, "Initial charging check failed", e);
        }
    }

    private void onChargingStateObserved(
            boolean charging, String source) {

        if (lastChargingState != null
                && lastChargingState == charging) {
            Log.d(TAG, "Duplicate state ignored: "
                    + charging + ", source=" + source);
            return;
        }

        Boolean previous = lastChargingState;
        lastChargingState = charging;

        Log.i(TAG, "Charging state transition: "
                + previous + " -> " + charging
                + ", source=" + source);

        if (!charging) {
            Log.d(TAG, "Charging disconnected; ready for next connection");
            return;
        }

        requestChargingStandby(source);
    }

    private void requestChargingStandby(String source) {
        if (!FocusManager.isAutoStandbyOnCharge(this)) {
            Log.i(TAG, "Auto StandBy disabled; launch skipped");
            return;
        }

        long now = SystemClock.elapsedRealtime();

        if (lastChargeLaunchElapsed != 0L
                && now - lastChargeLaunchElapsed
                < CHARGE_DEBOUNCE_MS) {
            Log.i(TAG, "Duplicate launch suppressed");
            return;
        }

        lastChargeLaunchElapsed = now;

        Log.i(TAG, "Requesting StandBy launch, source=" + source);

        try {
            FocusManager.openStandbyClock(
                    this, true, "charging");

            // A successful call does not prove the activity appeared.
            Log.i(TAG, "StandBy startActivity request submitted");

        } catch (RuntimeException e) {
            Log.e(TAG, "StandBy launch threw exception", e);
        }
    }

    // Existing idle StandBy functionality.
    private final Runnable idleCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (FocusManager.shouldAutoStartFromIdle(
                    FocusAccessibilityService.this)) {
                try {
                    FocusManager.openStandbyClock(
                            FocusAccessibilityService.this,
                            true, "idle");
                } catch (Exception ignored) {
                    // Manual standby remains available.
                }
            }

            handler.postDelayed(this, 15_000L);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        Log.i(TAG, "Accessibility service connected");

        FocusManager.recordUserActivity(this);

        FocusNotificationHelper.prepare(this);
        FocusNotificationHelper.update(this);

        handler.removeCallbacks(idleCheckRunnable);
        handler.post(idleCheckRunnable);

        registerChargingMonitor();
        checkInitialChargingState();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        FocusNotificationHelper.update(this);

        if (event == null) return;

        FocusManager.recordUserActivity(this);

        if (event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (packageName.equals(getPackageName())) return;

        if (!FocusManager.isFocusActive(this)) return;
        if (!FocusManager.isBlocked(this, packageName)) return;

        long now = System.currentTimeMillis();

        if (packageName.equals(lastBlockedPackage)
                && now - lastBlockLaunch < 1200) return;

        lastBlockedPackage = packageName;
        lastBlockLaunch = now;

        Intent intent = new Intent(this, BlockActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        intent.putExtra("blockedPackage", packageName);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Accessibility service destroyed");

        unregisterChargingMonitor();
        handler.removeCallbacks(idleCheckRunnable);

        super.onDestroy();
    }
}
