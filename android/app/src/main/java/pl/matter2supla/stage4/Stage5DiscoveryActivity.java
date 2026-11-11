package pl.matter2supla.stage4;

import android.app.Activity;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stage 5A: read-only discovery of commissionable Matter devices on local Wi-Fi. */
public final class Stage5DiscoveryActivity extends Activity {
    private static final String TAG = "Matter2SuplaStage5";
    private static final String SERVICE_TYPE = "_matterc._udp.";
    private NsdManager nsd;
    private NsdManager.DiscoveryListener discoveryListener;
    private WifiManager.MulticastLock multicastLock;
    private boolean discoveryRunning;
    private TextView output;
    private Button toggle;
    private final Map<String, String> discovered = new LinkedHashMap<>();
    private String status = "Gotowy do skanowania.";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        nsd = (NsdManager) getSystemService(NSD_SERVICE);
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("Matter2SuplaStage5-MDNS");
            multicastLock.setReferenceCounted(false);
        }
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(24, 24, 24, 24);
        TextView heading = new TextView(this);
        heading.setText("Matter2Supla — Stage 5A\nWykrywanie Matter przez Wi-Fi (mDNS)");
        heading.setTextSize(20);
        main.addView(heading);
        toggle = new Button(this);
        toggle.setText("Rozpocznij skanowanie");
        toggle.setOnClickListener(v -> { if (discoveryRunning) stopDiscovery(); else startDiscovery(); });
        main.addView(toggle);
        ScrollView scroll = new ScrollView(this);
        output = new TextView(this);
        output.setTextSize(15);
        output.setTextIsSelectable(true);
        output.setPadding(8, 20, 8, 20);
        scroll.addView(output);
        main.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(main);
        redraw();
    }

    private void startDiscovery() {
        if (discoveryListener != null) return;
        if (nsd == null) { status = "BŁĄD: brak NsdManager"; redraw(); return; }
        discovered.clear();
        status = "Rozpoczynam wyszukiwanie " + SERVICE_TYPE;
        if (multicastLock != null && !multicastLock.isHeld()) multicastLock.acquire();
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                status = "BŁĄD STARTU NSD: " + errorCode;
                Log.e(TAG, status);
                discoveryRunning = false;
                cleanupAfterFailedStart();
                redraw();
            }
            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                status = "BŁĄD ZATRZYMANIA NSD: " + errorCode;
                Log.e(TAG, status);
                discoveryRunning = false;
                discoveryListener = null;
                releaseMulticast();
                redraw();
            }
            @Override public void onDiscoveryStarted(String serviceType) {
                discoveryRunning = true;
                status = "Skanowanie aktywne: " + serviceType;
                Log.i(TAG, status);
                redraw();
            }
            @Override public void onDiscoveryStopped(String serviceType) {
                discoveryRunning = false;
                discoveryListener = null;
                status = "Skanowanie zatrzymane.";
                releaseMulticast();
                redraw();
            }
            @Override public void onServiceFound(NsdServiceInfo service) {
                if (service == null) return;
                String name = service.getServiceName();
                discovered.put(name, "Znaleziono: " + name + "\nTyp: " + service.getServiceType());
                Log.i(TAG, "Matter commissionable service found: " + name);
                redraw();
            }
            @Override public void onServiceLost(NsdServiceInfo service) {
                if (service == null) return;
                discovered.remove(service.getServiceName());
                Log.i(TAG, "Matter commissionable service lost: " + service.getServiceName());
                redraw();
            }
        };
        try { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener); }
        catch (RuntimeException err) {
            Log.e(TAG, "Discovery start exception", err);
            status = "BŁĄD: " + err;
            cleanupAfterFailedStart();
            redraw();
        }
    }

    private void cleanupAfterFailedStart() {
        discoveryListener = null;
        releaseMulticast();
    }

    private void stopDiscovery() {
        if (discoveryListener == null) return;
        try { nsd.stopServiceDiscovery(discoveryListener); }
        catch (RuntimeException err) {
            Log.e(TAG, "Discovery stop exception", err);
            status = "BŁĄD: " + err;
            discoveryRunning = false;
            discoveryListener = null;
            releaseMulticast();
            redraw();
        }
    }
    private void releaseMulticast() {
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
    }
    private void redraw() {
        if (!isFinishing()) runOnUiThread(() -> {
            toggle.setText(discoveryRunning ? "Zatrzymaj skanowanie" : "Rozpocznij skanowanie");
            StringBuilder b = new StringBuilder(status);
            b.append("\n\nUrządzenia w trybie parowania: ").append(discovered.size()).append("\n");
            for (String line : discovered.values()) b.append("\n").append(line).append("\n");
            b.append("\nUwaga: brak urządzeń nie musi oznaczać problemu z tabletem. ")
             .append("Urządzenie Matter musi być w trybie parowania i widoczne w tej sieci.\n")
             .append("Test tylko odczytu mDNS. Nie paruje urządzeń, nie inicjalizuje kontrolera i nie modyfikuje SUPLA(t) ani Zigbee Stage 8.\n")
             .append("Build: 11.11.2026 @lukasz06");
            output.setText(b.toString());
        });
    }
    @Override protected void onDestroy() {
        if (discoveryListener != null) {
            try { nsd.stopServiceDiscovery(discoveryListener); } catch (RuntimeException ignored) { }
            discoveryListener = null;
        }
        releaseMulticast();
        super.onDestroy();
    }
}
