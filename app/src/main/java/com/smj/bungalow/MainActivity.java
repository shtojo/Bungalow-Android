package com.smj.bungalow;

// Select BUILD > GENERATE SIGNED BUNDLE/APK, check APK then NEXT
// Use existing keystore (...AndroidClient/keystore/keystore.jks) or could create new
// Use 'smjbungalow' for keystore and both passwords, if creating new, set years to 25
// Select 'release' (or 'debug') and 'V2', set desired destination folder then finish.
// Copy apk to device, use file explorer, select to install, must allow 'unknown sources'

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.DataFormatException;

public class MainActivity extends AppCompatActivity {

    enum NetworkResult {
        CONNECTED, // Connected successfully
        MESSAGE_RECEIVED, // Message was received from server
        NO_NETWORK, // No network connection (neither wifi nor cellular)
        ENCRYPTION_ERROR, // Failed to generate the secure encryption key
        AUTHENTICATION_ERROR, // Failed authentication handshake with server
        NO_RESPONSE, // Server did not reply
        COMMUNICATION_ERROR   // Error communicating with the server
    }

    // These are stored on the server
    private static String emailPort = "";
    private static String emailServer = "";
    private static String emailPassword = "";
    private static String alarmContacts = "";

    private boolean isConnected = false;

    public StoredSettings storedSettings = null;

    // Keep fragment references instead of recreating when swiping
    public SecurityFragment securityFragment = null;
    public ZonesFragment zonesFragment = null;
    public EventsFragment eventsFragment = null;
    public RulesFragment rulesFragment = null;

    NetworkMonitor networkMonitor = null;

    // For action-bar progress spinner (busy indicator)
    //public MenuItem menuBusy = null;
    ProgressBar busyBar = null;

    private Socket socket;
    private static final int SOCKET_TIMEOUT = 5000; // milliseconds
    private BufferedReader reader;
    private PrintWriter writer;

    // networkType is used by SecurityFragment
    //public NetworkMonitor.activeNetworkType networkType = NetworkMonitor.activeNetworkType.NONE;

    // Initialize to empty string array, will fill when I get info from the server
    private String[] phrases = new String[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set main view to main_activity.xml
        setContentView(R.layout.main_activity);

        busyBar = findViewById(R.id.busybar);
        busyBar.setVisibility(View.INVISIBLE);

        // Retrieve saved settings
        storedSettings = new StoredSettings(this);
        storedSettings.Retrieve();

        // Request permissions if not already granted
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        // Start network monitoring
        networkMonitor = new NetworkMonitor(this);

        // Create fragments
        securityFragment = SecurityFragment.newInstance();
        zonesFragment = ZonesFragment.newInstance();
        eventsFragment = EventsFragment.newInstance();
        rulesFragment = RulesFragment.newInstance();

        // Create a ViewPager which allows swiping between pages
        // Connect it to a PagerAdapter which will tell it which page to show as swiped
        // Note: The TabPagerAdapter class is defined in this file
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager()));
        // set to number of tabs - 1, so never has to recreate the view when swiping left/right
        viewPager.setOffscreenPageLimit(3);

        // Connect the tabs (defined in main_activity.xml) to the ViewPager
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    // Called after onCreate and also after app is restarted
    // Unlike onResume which is called whenever the app goes to foreground
    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the server
        connect(true);
    }

    private void connect(boolean prompt) {
        if (prompt) {
            // This works
            //View view = ((LayoutInflater) Objects.requireNonNull(
            //        getSystemService(Context.LAYOUT_INFLATER_SERVICE)))
            //        .inflate(R.layout.connect, null, false);

            // This also works
            final View view = View.inflate(this, R.layout.connect, null);

            // Find dialog items
            final EditText password_et = view.findViewById(R.id.connect_password);
            final Switch settingsSwitch = view.findViewById(R.id.connect_switch);
            final EditText local_et = view.findViewById(R.id.connect_local);
            final EditText remote_et = view.findViewById(R.id.connect_remote);
            final EditText port_et = view.findViewById(R.id.connect_port);
            final TextView local_tv = view.findViewById(R.id.connect_localText);
            final TextView remote_tv = view.findViewById(R.id.connect_remoteText);
            final TextView port_tv = view.findViewById(R.id.connect_portText);
            final CheckBox home_cb = view.findViewById(R.id.connect_onHomeWifi);
            final TextView wifi_name_tv = view.findViewById(R.id.connect_currentName);
            final TextView wifi_mac_tv = view.findViewById(R.id.connect_currentMac);
            final TextView connection_tv = view.findViewById(R.id.connect_connection);

            // Collapse
            local_et.setVisibility(View.GONE);
            remote_et.setVisibility(View.GONE);
            port_et.setVisibility(View.GONE);
            local_tv.setVisibility(View.GONE);
            remote_tv.setVisibility(View.GONE);
            port_tv.setVisibility(View.GONE);
            home_cb.setVisibility(View.GONE);
            wifi_name_tv.setVisibility(View.GONE);
            wifi_mac_tv.setVisibility(View.GONE);
            connection_tv.setVisibility(View.GONE);
            settingsSwitch.setText("Show Connection Settings");

            // Fill dialog with the current settings
            password_et.setText(storedSettings.password);
            local_et.setText(storedSettings.localServer);
            remote_et.setText(storedSettings.remoteServer);
            port_et.setText(String.valueOf(storedSettings.serverPort));
            port_et.setInputType(InputType.TYPE_CLASS_NUMBER);
            if (networkMonitor.isWifi()) {
                connection_tv.setText("Will connect via wifi");
                String currentMac = networkMonitor.getMacAddress();
                wifi_mac_tv.setText(currentMac);
                wifi_name_tv.setText(networkMonitor.getWifiName());
                if (currentMac.equals(storedSettings.homeMac)){
                    home_cb.setChecked(true);
                }
                else{
                    home_cb.setChecked(false);
                }
            }
            else if (networkMonitor.isCellular()) {
                connection_tv.setText("Will connect via cellular");
            }
            else {
                connection_tv.setText("No Network Connection!");
            }

            settingsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // Expand
                        local_et.setVisibility(View.VISIBLE);
                        remote_et.setVisibility(View.VISIBLE);
                        port_et.setVisibility(View.VISIBLE);
                        local_tv.setVisibility(View.VISIBLE);
                        remote_tv.setVisibility(View.VISIBLE);
                        port_tv.setVisibility(View.VISIBLE);
                        connection_tv.setVisibility(View.VISIBLE);
                        settingsSwitch.setText("Hide Connection Settings");
                        if (networkMonitor.isWifi()) {
                            wifi_mac_tv.setVisibility(View.VISIBLE);
                            wifi_name_tv.setVisibility(View.VISIBLE);
                            home_cb.setVisibility(View.VISIBLE);
                        }

                    } else {
                        // Collapse
                        local_et.setVisibility(View.GONE);
                        remote_et.setVisibility(View.GONE);
                        port_et.setVisibility(View.GONE);
                        local_tv.setVisibility(View.GONE);
                        remote_tv.setVisibility(View.GONE);
                        port_tv.setVisibility(View.GONE);
                        home_cb.setVisibility(View.GONE);
                        wifi_name_tv.setVisibility(View.GONE);
                        wifi_mac_tv.setVisibility(View.GONE);
                        connection_tv.setVisibility(View.GONE);
                        settingsSwitch.setText("Show Connection Settings");
                    }
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Connect to Server")
                    .setIcon(R.drawable.key)
                    .setCancelable(false)  // modal
                    .setView(view);

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Save the updated settings
                    String password = password_et.getText().toString();
                    if (((CheckBox) (view.findViewById(R.id.connect_savePassword))).isChecked()) {
                        storedSettings.password = password;
                    } else {
                        storedSettings.password = "";  // if unchecked then clear stored password
                    }
                    if (home_cb.isChecked()) {
                        storedSettings.homeMac = wifi_mac_tv.getText().toString();
                    }
                    else {
                        if (wifi_mac_tv.getText().equals(storedSettings.homeMac)){
                            // if here then user unchecked the home wifi box
                            // so clear the stored setting
                            storedSettings.homeMac = "";
                        }
                    }
                    storedSettings.localServer = local_et.getText().toString();
                    storedSettings.remoteServer = remote_et.getText().toString();
                    storedSettings.serverPort = Integer.parseInt(port_et.getText().toString());
                    storedSettings.Save();
                    // Try to connect to the server
                    new NetworkTask(password, true).execute();
                }
            });

            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            builder.create().show();
            password_et.requestFocus();
        }
        else {
            // connect without prompt (using saved settings)
            new NetworkTask(storedSettings.password, true).execute();
        }
    }

    // Disconnect from the server
    void disconnect() {
        // Note: If multiple streams are chained together then closing the one which was the last to
        // be constructed (outermost stream) will automatically close all of the underlying streams.
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (Exception e) {
            // just flow through without error
        }
        isConnected = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action-bar/menu item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        AlertDialog.Builder builder;

        switch (item.getItemId()) {

            // SPEAK
            case R.id.action_speak:

                //final View sp = LayoutInflater.from(this).inflate(R.layout.speak, null);
                final View sp = LayoutInflater.from(this).inflate(R.layout.speak,
                        (ViewGroup) item.getActionView(), false);

                builder = new AlertDialog.Builder(this)
                    .setTitle("Speak")
                    .setIcon(R.drawable.ic_action_volume_on)
                    .setView(sp);

                final SeekBar speakVolumeBar = sp.findViewById(R.id.speakVolume);
                final TextView speakVolumeText = sp.findViewById(R.id.speakVolumeText);

                final Spinner speakPhraseSpinner = sp.findViewById(R.id.speakPhraseSpinner);
                ArrayAdapter<String> phraseAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, phrases);
                phraseAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item); // The drop down view
                speakPhraseSpinner.setAdapter(phraseAdapter);

                speakVolumeBar.setMax(10);
                speakVolumeBar.setProgress(5);  // start at half volume
                speakVolumeText.setText(getString(R.string.volume, 5));

                SeekBar.OnSeekBarChangeListener volumeSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBark, int progress, boolean fromUser) {
                        speakVolumeText.setText(getString(R.string.volume, progress));
                    }
                };

                speakVolumeBar.setOnSeekBarChangeListener(volumeSeekBarListener);

                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String str = "SAY=" + Tools.encodeB80(speakVolumeBar.getProgress())
                                + Tools.encodeB80(speakPhraseSpinner.getSelectedItemPosition());
                        send(str);
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.show(); // note: same as calling builder.create().show();
                break;

            // MAIL SETTINGS
            case R.id.action_mail: // email contacts
                final View sv = LayoutInflater.from(this).inflate(R.layout.settings,
                        (ViewGroup) item.getActionView(), false);

                builder = new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_action_settings)
                    .setTitle("Settings")
                    .setView(sv);

                final EditText alarmContactsEdit = sv.findViewById(R.id.setupAlarmContacts);
                final EditText emailServerEdit = sv.findViewById(R.id.setupEmlServer);
                final EditText emailPasswordEdit = sv.findViewById(R.id.setupEmlPassword);

                final Spinner emailModeSpinner = sv.findViewById(R.id.setupEmlMode);
                ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
                        R.array.email_modes, android.R.layout.simple_spinner_item);
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                emailModeSpinner.setAdapter(adapter1);

                // server: 'N' = normal/25, 'S' = SSL/465, 'T' = TLS/587
                emailServerEdit.setText(emailServer);
                if (emailPort.equals("T")) emailModeSpinner.setSelection(2);
                else if (emailPort.equals("S")) emailModeSpinner.setSelection(1);
                else emailModeSpinner.setSelection(0);
                emailPasswordEdit.setText(emailPassword);
                alarmContactsEdit.setText(alarmContacts);

                // Email Test Button
                final Button emailBtn = sv.findViewById(R.id.emailTestBtn);
                emailBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Tells server to send test email for burglary and fire to all recipients in the email
                        // account list. Sends via primary, falls back to secondary email account on fail. To fully
                        // test, mess-up the primary and verify the send still completes (using secondary account)
                        send("EMT");
                    }
                });

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // MAS=from1~serv1~pw1~port1~from2~serv2~pw2~port2~contacts
                        // servx: 'N' = normal/25, 'S' = SSL/465, 'T' = TLS/587
                        StringBuilder sb = new StringBuilder();
                        int mode;
                        sb.append("EMS=")
                            .append(emailServerEdit.getText().toString()).append("~")
                            .append(emailPasswordEdit.getText().toString()).append("~");
                        mode = emailModeSpinner.getSelectedItemPosition();
                        if (mode == 2) sb.append("T~"); // T = TLS/587
                        else if (mode == 1) sb.append("S~"); // S = SSL/465
                        else sb.append("N~"); // N = NORMAL/25
                        sb.append(alarmContactsEdit.getText().toString());

                        try {
                            buildMailSettings(sb.toString()); // update local settings
                        } catch (DataFormatException e) {
                            // e.printStackTrace();
                            Toast.makeText(MainActivity.this,
                                    "Error building mail settings",
                                    Toast.LENGTH_SHORT).show();
                        }
                        send(sb.toString());
                    }
                });

                builder.setNegativeButton("Cancel", null)
                    .show(); // note: same as calling builder.create().show();
                break;

            // ACTIVATE ALARM
            case R.id.action_alarm:
                send("ALM");
                break;

            // REBOOT SERVER
            case R.id.action_reboot:
                send("RBT");
                break;

            // QUIT APPLICATION
            case R.id.action_quit:
                finish();
                break;

            // ABOUT
            case R.id.action_about:
                builder = new AlertDialog.Builder(this)
                    .setTitle("Bungalow")
                    .setIcon(R.drawable.ic_action_about)
                    .setMessage("Created by Shawn Johnston")
                    .setPositiveButton("Ok", null);
                builder.show();
                break;

            default:
                return super.onOptionsItemSelected(item); // Call this if menu item unhandled, returns false
        }
        return true; // Return true to indicate successfully handled menu item
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();  // calls error on exit ??????????????????????????????????????????????
        networkMonitor.stop();
        disconnect();
    }

    //@Override
    //public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    //    // When tab is selected switch to the corresponding page in the ViewPager.
    //    mViewPager.setCurrentItem(tab.getPosition());
    //}

    // ARM STAY BUTTON CLICK HANDLER
    public void securityArmStayButtonClick(View view) {
        send("STY");
    }

    // ARM AWAY BUTTON CLICK HANDLER
    public void securityArmAwayButtonClick(View view) {
        send("AWY");
    }

    // DISARM BUTTON CLICK HANDLER
    public void securityDisarmButtonClick(View view) {
        // Note: returns immediately (the callback is invoked on completion)
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance()); // password style

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle("Bungalow")
            .setIcon(R.drawable.password)
            .setMessage("Enter code to disarm")
            .setView(input)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                        send("DIS=" + input.getText().toString());
                    }
                })
            .setNegativeButton("Cancel", null);

        // show the keyboard
        final AlertDialog dialog = builder.create(); // get the AlertDialog object

        // Add a text change listener to the edit control
        // So I can enable the OK button after numeric password is entered
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int i, int i2, int i3) {}
            @Override public void onTextChanged(CharSequence c, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable str) {
                int len = str.toString().length();
                if ((len >= 4) && (len <= 6) && (isNumeric(str.toString()))) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        // set initial button state to disabled (must be after dialog.show)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        input.requestFocus();
    }

    // HANDLE RESPONSES FROM THE SERVER
    public void HandleNetworkMessage(NetworkResult result, String message) {
        String errMsg = null; // if not null at end then will show message dialog
        //boolean prompt = false;  // prompt on reconnect (if not ExitProgram)?
        boolean reconnect = false;  // true = reconnect, false = exit

        switch (result) {
            case MESSAGE_RECEIVED:

                // QRY = Query
                if (message.startsWith("QRY=")) {
                    String[] parts = message.split("\\$", -1);
                    if (parts.length != 3) {
                        errMsg = "Invalid QRY response! " + message;
                        break;
                    }
                    parts[0] = parts[0].substring(4);  // skip QRY=

                    try {
                        SecurityFragment.buildSecurity(parts[0]);
                        ZonesFragment.buildZones(parts[1]);
                        EventsFragment.buildEvents(parts[2]);
                    } catch (DataFormatException e) {
                        errMsg = e.getMessage();
                        break;
                    }
                }

                // QRA = Query all
                else if (message.startsWith("QRA=")) {
                    String[] parts = message.split("\\$", -1);
                    if (parts.length != 7) {
                        errMsg = "Invalid QRA response length (exp 7, act " + parts.length + "! " + message;
                        break;
                    }
                    parts[0] = parts[0].substring(4); // skip QRA=

                    try {
                        buildMailSettings(parts[0]);
                        ZonesFragment.saveZoneNamesAndTypes(parts[1]);
                        phrases = parts[2].split("~"); // populate speak phrase spinner
                        RulesFragment.buildRules(parts[3]);
                        SecurityFragment.buildSecurity(parts[4]);
                        ZonesFragment.buildZones(parts[5]);
                        EventsFragment.buildEvents(parts[6]);
                    } catch (DataFormatException e) {
                        errMsg = e.getMessage();
                        break;
                    }
                }

                // OK
                else if (message.equals("SAY=OK"))
                    Toast.makeText(this, "Speaking...", Toast.LENGTH_SHORT).show();
                else if (message.equals("ALM=OK"))
                    Toast.makeText(this, "Alarm activated!", Toast.LENGTH_SHORT).show();
                else if (message.equals("EMT=OK"))
                    Toast.makeText(this, "Mail sent.", Toast.LENGTH_SHORT).show();
                else if (message.equals("RBT=OK")) { // Server response to reboot server button
                    Toast.makeText(this, "Rebooting...", Toast.LENGTH_SHORT).show();
                    errMsg = "Server is rebooting...";
                    reconnect = false;  // will show message then exit program below
                }

                // ER
                else if (message.equals("STY=ER"))
                    errMsg = "Server reported an error with while arming in stay mode!";
                else if (message.equals("AWY=ER"))
                    errMsg = "Server reported an error while arming in away mode!";
                else if (message.equals("DIS=ER"))
                    errMsg = "Server reported an error while disarming!";
                else if (message.equals("EMS=ER"))
                    errMsg = "Server reported an error while processing email settings!";
                else if (message.equals("SAY=ER"))
                    errMsg = "Server reported an error while processing speak command!";
                else if (message.equals("ALM=ER"))
                    errMsg = "Server reported an error while triggering alarm!";
                else if (message.equals("ZTC=ER"))
                    errMsg = "Server reported an error while processing zone type change command!";
                else if (message.equals("RUL=ER"))
                    errMsg = "Server reported an error while processing the rule!";
                else if (message.equals("EMT=ER"))
                    errMsg = "Server reported an error while performing email test!";

                // ERR
                else if (message.equals("ERR"))
                    errMsg = "Server received an invalid command from the client!";

                // ?
                else errMsg = "Invalid response from the server: " + message;
                break;

            case CONNECTED:
                isConnected = true;
                // Display a message indicating the connection type that was made
                if (networkMonitor.isWifi()) {
                    if (networkMonitor.getMacAddress().equals(storedSettings.homeMac)){
                        Toast.makeText(this, "Connected via home wifi", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(this, "Connected via remote wifi", Toast.LENGTH_LONG).show();
                    }
                }
                else if (networkMonitor.isCellular()) {
                    Toast.makeText(this, "Connected via cellular", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
                }
                send("QRA");  // query all (get complete update from server)
                break;

            case NO_NETWORK:
            case COMMUNICATION_ERROR:
                isConnected = false;
                errMsg = message;  // show message
                reconnect = false;  // exit program
                break;

            case AUTHENTICATION_ERROR:
            case ENCRYPTION_ERROR:
                isConnected = false;
                errMsg = message;  // show message
                reconnect = true;  // reconnect with prompt
                break;

            case NO_RESPONSE:
                isConnected = false;
                errMsg = null;  // don't show message
                reconnect = true;  // reconnect without prompt
                // show reconnecting toast message
                break;
        }

        securityFragment.updateView(isConnected);
        zonesFragment.updateView();
        eventsFragment.updateView();
        rulesFragment.updateView();

        if (errMsg != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Bungalow")
                    .setIcon(R.drawable.error)
                    .setMessage(errMsg);
            if (reconnect) {
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        connect(true);
                    }
                });
            } else {
                builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                });
            }
            builder.show();
        }
        else if (reconnect) {
            // Reconnect without prompt, show toast message
            Toast.makeText(this, "Re-connecting...", Toast.LENGTH_SHORT).show();
            connect(false);
        }
    }

    /*public void BusyIndicator(boolean busy) {
        if (busy) {
            menuBusy.setActionView(R.layout.busybar);
            menuBusy.expandActionView();
        }
        else {
            menuBusy.collapseActionView();
            menuBusy.setActionView(null);
        }
    }*/

    //Populate the mail settings variables with data from server
    // @param s string received from server
    // @return null on success, error message on failure
    //
    private static void buildMailSettings(String s) throws DataFormatException {
        //Log.v("SETTINGS", s);
        String[] words = s.split("~", -1); // -1 = do not discard trailing consecutive delimiters
        if (words.length != 4) {
            throw new DataFormatException("Invalid mail settings (" + s + ")");
        }
        emailServer = words[0];
        emailPassword = words[1];
        emailPort = words[2];
        alarmContacts = words[3];
    }

    // TabPagerAdapter Class
    public class TabPagerAdapter extends FragmentPagerAdapter {

        TabPagerAdapter(FragmentManager fm) {
            super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Returns a fragment, note that getItem is not always called when switching
            // pages if the item was already created and still exists, then it will
            // be retrieved from the FragmentManager instead.
            switch (position) {
                case 0: // SECURITY
                    return securityFragment;
                case 1: // ZONES
                    return zonesFragment;
                case 2: // EVENTS
                    return eventsFragment;
                case 3: // RULES
                    return rulesFragment;
                default:
                    throw new RuntimeException();  // should never get here
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECURITY";
                case 1:
                    return "ZONES";
                case 2:
                    return "EVENTS";
                case 3:
                    return "RULES";
                default:
                    throw new RuntimeException();  // should never get here
            }
        }

        @Override
        public int getCount() {
            return 4;  // 4 tabs
        }
    }

    // SEND COMMAND TO SERVER
    void send(String netMsg) {
        new NetworkTask(netMsg, false).execute();
    }

    /**
     * Determine if string is numeric
     */
    static boolean isNumeric(String str) {
        try {
            // Don't need the resulting value, just need to see if parses
            Integer.parseInt(str);
        }
        catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**********************************
     ********** NETWORK TASK **********
     **********************************/

    /*
    IF STATIC CLASS

        private WeakReference<MainActivity> activityReference;
        // Retain only a weak reference to the activity

        private NetworkTask(String message, boolean connect, MainActivity context) {
            this.task = task;
            this.messageIn = message;
            this.activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() { // runs in main ui thread
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;
            activity.busyBar.setVisibility(View.VISIBLE);
        }
     */
    private class NetworkTask extends AsyncTask<Void, String, NetworkResult> {
        // parameters = param, progress, result
        // Note: I don't use the 'param' but instead use a constructor to pass args
        private String messageIn;   // password if connecting, else message to send
        private String messageOut = "";  // response message
        private boolean connect;  // true = connect with password, false = send message

        private NetworkTask(String message, boolean connect) {
            this.connect = connect;
            this.messageIn = message;
        }

        //private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        //public NetworkConnectTask(MainActivity context, char[] password) {
        //    activityReference = new WeakReference<>(context);
        //    this.password = password;
        //}

        @Override
        protected void onPreExecute() { // runs in main ui thread
            // If already showing the swipe-refresh busy indicator, then don't show the
            // busy indicator at center of screen
            boolean isSwipe = false;
            if (securityFragment.swipeLayout !=  null) {
                if (securityFragment.swipeLayout.isRefreshing()) isSwipe = true;
            }
            if (eventsFragment.swipeLayout !=  null) {
                if (eventsFragment.swipeLayout.isRefreshing()) isSwipe = true;
            }
            if (!isSwipe) busyBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected NetworkResult doInBackground(Void... v) {

            if (networkMonitor.isNone()) {
                messageOut = "No network available!\nConnect to a network then retry.";
                return NetworkResult.NO_NETWORK;
            }

            if (connect) {  // connect to server
                byte[] salt = {(byte) 0x1d, (byte) 0x2e, (byte) 0x4f, (byte) 0x37,
                        (byte) 0x8c, (byte) 0xc5, (byte) 0x3e, (byte) 0xf6};

                disconnect();  // ensure previous connection closed

                String ip = storedSettings.remoteServer;
                // CHANGE THE LINE BELOW TO USE MAC INSTEAD OF SSID NAME
                if (networkMonitor.isWifi()) {
                    if (networkMonitor.getMacAddress().equals(storedSettings.homeMac)) {
                        ip = storedSettings.localServer;
                    }
                }

                // Generate encryption key (do this in the background thread since may take a bit)
                try {
                    Crypto.generateSecureKey(messageIn.toCharArray(), salt, Crypto.Mode.AES128);
                } catch (Exception e) {
                    messageOut = "Unable to generate encryption key!\n\n" + e.getMessage();
                    return NetworkResult.ENCRYPTION_ERROR;
                }

                // Connect and initialize the reader/writer streams
                socket = new Socket();
                try {
                    // Set the socket read timeout, reading a stream on this socket
                    // will then block for this time max else SocketTimeoutException
                    socket.setSoTimeout(SOCKET_TIMEOUT); // set socket read timeout in ms

                    // Connect, throws exception if can't connect.
                    // getByName accepts dotted ip address or host name
                    socket.connect(new InetSocketAddress(
                            InetAddress.getByName(ip), storedSettings.serverPort), SOCKET_TIMEOUT);

                    reader = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));

                    writer = new PrintWriter(new OutputStreamWriter(
                            socket.getOutputStream(), StandardCharsets.UTF_8), true);
                } catch (Exception e) {
                    messageOut = "Unable to connect to server!\nCheck network settings.\n\n"
                            + e.getMessage();
                    return NetworkResult.COMMUNICATION_ERROR;
                }

                // Authentication handshake with server
                try {
                    writer.println(Crypto.encrypt("AuthRequest")); // send

                    // readLine returns null if no chars available or exception if reader is closed
                    String response = reader.readLine(); // receive
                    response = Crypto.decrypt(response);

                    if ((response == null) || (!response.equals("AuthGrant"))) {
                        messageOut = "Authentication error!\nCheck password and retry.";
                        return NetworkResult.AUTHENTICATION_ERROR;
                    }
                } catch (Exception e) {
                    // readline: IOException (if reader is closed or io error)
                    messageOut = "Unable to connect to server!\nCheck network settings.\n\n"
                            + e.getMessage();
                    return NetworkResult.COMMUNICATION_ERROR;
                }
                messageOut = "Connected to server.";
                return NetworkResult.CONNECTED;
            }

            // Send Message (connect == false)
            else {
                try {
                    writer.println(Crypto.encrypt(messageIn));
                    // readLine blocks until data received or timeout
                    // returns null if no data else exception if connection closed
                    messageOut = reader.readLine();
                    if (messageOut == null) {
                        messageOut = "No response from server! Server may have closed the connection.";
                        return NetworkResult.NO_RESPONSE;
                    }
                    messageOut = Crypto.decrypt(messageOut);
                } catch (Exception e) {
                    messageOut = "Error communicating with server!\n\n" + e.getMessage();
                    return NetworkResult.COMMUNICATION_ERROR;
                }
                return NetworkResult.MESSAGE_RECEIVED;
            }
        }

        @Override
        protected void onPostExecute(final NetworkResult result) {
            busyBar.setVisibility(View.INVISIBLE);
            // if user swiped down on security page to refresh then stop the refresh animation
            if (securityFragment.swipeLayout !=  null) {
                securityFragment.swipeLayout.setRefreshing(false);
            }
            // if user swiped down on events page to refresh then stop the refresh animation
            if (eventsFragment.swipeLayout !=  null) {
                eventsFragment.swipeLayout.setRefreshing(false);
            }
            HandleNetworkMessage(result, messageOut);
        }
    }
}

/*
 * Network protocol:
 *
 * Data is AES encrypted. A unique key is generated on both the server and the client. This key is
 * generated from a password or pass-phrase that must be shared between the server and client. A
 * unique 'salt' identifier is used to combine with the password and hashed to create a the secure
 * key. This salt makes the key more secure to avoid 'dictionary attacks' if common words are used
 * for the password. Both the password and the salt must be known to the server and the clients.
 *
 * Additionally, each client is required to identify itself to the server every time it establishes
 * a connection. It does this by sending a unique ClientId and the server maintains a list of
 * allowable clients. The client id can be entered in the menu. If any device fails to provide an
 * accepted ID to the server it will be disconnected. The ClientId does not need to be kept secure,
 * it is simply a string known to the server that is used to verify that the client knows how to
 * encrypt the data. Once decrypted by the server it is also used to identify the client
 *
 * Could use packet sequence numbers in the encrypted data and disconnect if not sequential. would
 * only make sense if i keep the connection open between packets.
 *
 *
 *
 * Note regarding UTF byte order marks: http://www.unicode.org/unicode/faq/utf_bom.html Java does
 * not recognize UTF Byte Order Marks (BOMs). UTF-8 can be indicated with leading EF-BB-BF (UTF-16
 * with FE-FF or FF-FE), etc Therefore, the application must recognize and remove these bytes or the
 * sender must be configured to not send them. I modified the server application to not use BOMs.
 *
 *
 *
 * Sequence when starting a new activity: Start activity A: A.onCreate, A.onStart, A.onResume... Now
 * start activity B: A.onPause, B.onCreate, B.onStart, B.onResume, A.onStop Now return from activity
 * B to activity A B.onPause, A.onStart, A.onResume, B.onStop, B.onDestroy
 *
 *
 *
 * See this link for application lifetime and the methods called during the activity
 * http://developer.android.com/reference/android/app/Activity.html
 *
 * Application lifetime = onCreate to onDestroy Visible lifetime = onStart to onStop Foreground
 * lifetime = onResume to onPause
 */

// For network command syntax see file "Bungalow Client Server Communication.txt"

// note for context, getApplicationContext() lives for life of application, this does not




/*
 * Determine network connection type
 * Could just try local first, but this takes a few seconds
 * This method quickly determines connection type.
 * @return
 *  NetworkType.NONE (0), WIFI_HOME (1), WIFI_AWAY (2), CELLULAR (3)
 */ /*
    private NetworkType findConnectionType(String homeSsid) {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {return NetworkType.NONE;}
        NetworkInfo netInfo  = cm.getActiveNetworkInfo();

        if ((netInfo == null) || (!netInfo.isConnected())) {
            //Toast.makeText(context, "No Network Connection!", Toast.LENGTH_SHORT).show();
            return NetworkType.NONE;
        }

        // if (netInfo.isRoaming())   could do this instead?
        if (netInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            //Toast.makeText(context, "On mobile Network", Toast.LENGTH_SHORT).show();
            return NetworkType.CELLULAR;
        }

        // Now I know I am on wifi, so check if on home network
        // Get ssid of network currently connected to
        WifiInfo wifiInfo = ((WifiManager) Objects.requireNonNull(getSystemService(Context.WIFI_SERVICE)))
                .getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid == null) {
            //Toast.makeText(context, "On remote wifi", Toast.LENGTH_SHORT).show();
            return NetworkType.WIFI_AWAY;
        }

        //Toast.makeText(context, "On wifi, ssid = " + ssid, Toast.LENGTH_SHORT).show();

        if (!ssid.equalsIgnoreCase(homeSsid)) {
            return NetworkType.WIFI_AWAY;
        }

        return NetworkType.WIFI_HOME;
    }*/