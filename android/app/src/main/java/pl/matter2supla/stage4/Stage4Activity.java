package pl.matter2supla.stage4;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;

/**
 * Stage 4: isolated native library loading test, NOT a Matter commissioner.
 * Does not pair, scan, use BLE, write to SUPLA or touch Zigbee/UART.
 */
public final class Stage4Activity extends Activity {
    private static final String TAG = "Matter2SuplaStage4";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text = new TextView(this);
        text.setTextSize(16);
        text.setTypeface(Typeface.MONOSPACE);
        text.setPadding(32, 32, 32, 32);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(text);
        setContentView(scroll);

        StringBuilder result = new StringBuilder();
        result.append("Matter2Supla — Stage 4\n")
              .append("Android API: ").append(Build.VERSION.SDK_INT).append('\n')
              .append("Supported ABI: ").append(java.util.Arrays.toString(Build.SUPPORTED_ABIS)).append("\n\n");

        try {
            // Check that the Java API packaged from Stage 2 is present.
            Class<?> api = Class.forName("chip.devicecontroller.ChipDeviceController", false, getClassLoader());
            result.append("CHIPController.jar: OK — ").append(api.getName()).append('\n');
        } catch (Throwable failure) {
            result.append("CHIPController.jar: ERROR — ").append(failure).append('\n');
            android.util.Log.e(TAG, "Java API unavailable", failure);
        }

        // Prevent JNI_OnLoad from aborting the process when essential Java
        // classes expected by the native Matter library are missing.
        boolean jniClassesPresent = true;
        for (String name : new String[] {
                "chip.devicecontroller.ChipDeviceControllerException",
                "chip.platform.AndroidChipPlatformException"
        }) {
            try {
                Class.forName(name, false, getClassLoader());
                result.append("JNI class: OK — ").append(name).append('\n');
            } catch (Throwable failure) {
                jniClassesPresent = false;
                result.append("JNI class: MISSING — ").append(name).append(" — ")
                      .append(failure).append('\n');
                android.util.Log.e(TAG, "Required JNI class unavailable: " + name, failure);
            }
        }

        if (!jniClassesPresent) {
            result.append("\nNative loading skipped to avoid a fatal JNI abort.\n");
            text.setText(result.toString());
            return;
        }

        try {
            // JNI_OnLoad executes here, so failures are reported, not hidden.
            System.loadLibrary("CHIPController");
            result.append("libCHIPController.so: LOADED\n");
            android.util.Log.i(TAG, "Native Matter controller library loaded");
        } catch (Throwable failure) {
            result.append("libCHIPController.so: ERROR — ").append(failure).append('\n');
            android.util.Log.e(TAG, "Matter JNI load failed", failure);
        }

        result.append("\nNo Matter controller has been initialized.\n")
              .append("No devices are paired. No service started.\n")
              .append("SUPLA(t) / Zigbee Stage 8 unchanged.\n")
              .append("Build: Stage 4.2 — 11.11.2026 @lukasz06\n");
        text.setText(result.toString());
    }
}
