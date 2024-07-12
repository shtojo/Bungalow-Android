package com.smj.bungalow;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.DataFormatException;

public class RulesFragment extends ListFragment {

    private ArrayAdapter<String> adapter;
    private static List<String> ruleList = new ArrayList<>(); // list of readable rules for display

    private Spinner when_spinner, when_zone_spinner, when_dayofweek_spinner;
    private Spinner when_date_spinner, when_hour_spinner;
    private Spinner when_minute_spinner, when_ampm_spinner, if_spinner1;
    private Spinner if_spinner2, do_spinner, do_device_spinner, do_speak_spinner;
    private EditText do_email_address, do_email_subject, do_email_body;
    private Spinner do_duration_spinner, do_duration_units_spinner;
	
    static RulesFragment newInstance(/*index*/) {
        return new RulesFragment();
    }

    // Note: onActivityCreated will always be run after onCreateView
	// Called when view recreated on tab switch to rebuild the view, but only if not still in mem
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // save root view to pass as parent view in AddNewRule
        return inflater.inflate(R.layout.rule_list, container, false);
    }

    // onResume is called when a fragment/page is loaded.
    // Typically the current page is cached as well as one page on each side.
    // So this is not called when a page comes into view, but when it is loaded to memory.    
    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    // setMenuVisibility is called with visible=true whenever a fragment/page is scrolled into view
    // This can be used to do processing on the fragment/page when it becomes visible to the user    
    //@Override
    //public void setMenuVisibility(final boolean visible) {
    //    super.setMenuVisibility(visible);
        // if (visible) {
        // RulesFragment is now visible
        // Toast.makeText(getActivity(), "setMenuVisibility", Toast.LENGTH_SHORT).show();
        // updateView();
        // }
    //}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                android.R.layout.simple_list_item_1, ruleList);
        setListAdapter(adapter);
    }

    void updateView() {
        // Update view if it is visible or cached (isVisible indicates the view is cached 
        // which means the current view or typically one view on each side)
        if (isVisible()) {
            adapter.notifyDataSetChanged();
        }
        // if (isVisible()) Toast.makeText(getActivity(), "RulesFragment.updateView", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onListItemClick(ListView l, @NonNull View v, final int index, long id) {
        // If user clicked the last item in the list (add new rule item)
        if (index == l.getCount() - 1) {
            AddNewRule();
        }
        // Else allow delete or cancel rule
        else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            dialogBuilder.setTitle("Bungalow");

            dialogBuilder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    adapter.notifyDataSetChanged();
                    ((MainActivity)Objects.requireNonNull(getActivity()))
                            .send("RUL-" + Tools.encodeB80(index));
                }
            });

            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            dialogBuilder.show();
        }
    }
		
		
    private void AddNewRule() {
        // Inflate the rule-add view

        //View setupView = ((LayoutInflater) Objects.requireNonNull(Objects.requireNonNull(
        //        getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(
        //                R.layout.rule_add, null,false);
        //View.inflate(getActivity(), R.layout.rule_add, false);

        //LayoutInflater inflater = LayoutInflater.from(getActivity());
        //inflater.inflate(R.layout.rule_add, view, false);
        View setupView = View.inflate(getActivity(), R.layout.rule_add, null);

        // Find the controls
        // Do this here (only when user selects 'add') instead of in the main fragment
        // Since most of the time the user will only be viewing/swiping across this screen
        // So reduce any delay by getting these only when 'add' is selected
        when_spinner = setupView.findViewById(R.id.ruleWhenSpinner);
        when_zone_spinner = setupView.findViewById(R.id.ruleWhenZoneSpinner);
        when_dayofweek_spinner = setupView.findViewById(R.id.ruleWhenDayofweekSpinner);
        when_date_spinner = setupView.findViewById(R.id.ruleWhenDateSpinner);
        when_hour_spinner = setupView.findViewById(R.id.ruleWhenHourSpinner);
        when_minute_spinner = setupView.findViewById(R.id.ruleWhenMinuteSpinner);
        when_ampm_spinner = setupView.findViewById(R.id.ruleWhenAmpmSspinner);
        if_spinner1 = setupView.findViewById(R.id.ruleIfSpinner1);
        if_spinner2 = setupView.findViewById(R.id.ruleIfSpinner2);
        do_spinner = setupView.findViewById(R.id.ruleDoSpinner);
        do_speak_spinner = setupView.findViewById(R.id.ruleDoSpeakSpinner);
        do_device_spinner = setupView.findViewById(R.id.ruleDoDeviceSpinner);
        do_email_address = setupView.findViewById(R.id.ruleDoEmlAddress);
        do_email_subject = setupView.findViewById(R.id.ruleDoEmlSubject);
        do_email_body = setupView.findViewById(R.id.ruleDoEmlbody);
        do_duration_spinner = setupView.findViewById(R.id.ruleDoDurationSpinner);
        do_duration_units_spinner = setupView
                .findViewById(R.id.ruleDoDurationUnitsSpinner);

        // Initialize the zone name spinners (when-zone & if-zone)
        String[] zoneNames = ZonesFragment.getZoneNamesAsStringArray();

        // Now use this string array to populate the spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                android.R.layout.simple_spinner_item, zoneNames);
        when_zone_spinner.setAdapter(adapter);
        if_spinner2.setAdapter(adapter); // reuse same zone adapter

        // WHEN selection changed listener
        when_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int pos, long id) {
                String sel = when_spinner.getSelectedItem().toString().toLowerCase(Locale.US);

                when_hour_spinner.setVisibility(View.GONE);
                when_minute_spinner.setVisibility(View.GONE);
                when_ampm_spinner.setVisibility(View.GONE);
                when_dayofweek_spinner.setVisibility(View.GONE);
                when_date_spinner.setVisibility(View.GONE);
                when_zone_spinner.setVisibility(View.GONE);

                // "zone faulted" or "zone ready"
                if ((sel.contains("zone"))) {
                    when_zone_spinner.setVisibility(View.VISIBLE);
                }

                // "daily"
                else if (sel.contains("daily")) {
                    when_hour_spinner.setVisibility(View.VISIBLE);
                    when_minute_spinner.setVisibility(View.VISIBLE);
                    when_ampm_spinner.setVisibility(View.VISIBLE);
                }

                // "weekly"
                else if (sel.contains("weekly")) {
                    when_hour_spinner.setVisibility(View.VISIBLE);
                    when_minute_spinner.setVisibility(View.VISIBLE);
                    when_ampm_spinner.setVisibility(View.VISIBLE);
                    when_dayofweek_spinner.setVisibility(View.VISIBLE);
                }

                // "monthly"
                else if (sel.contains("monthly")) {
                    when_hour_spinner.setVisibility(View.VISIBLE);
                    when_minute_spinner.setVisibility(View.VISIBLE);
                    when_ampm_spinner.setVisibility(View.VISIBLE);
                    when_date_spinner.setVisibility(View.VISIBLE);
                }

                // else "burglar alarm", "fire alarm", "armed", "armed away", "armed stay", "disarmed"
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                when_spinner.setSelection(0);
                when_hour_spinner.setVisibility(View.GONE);
                when_minute_spinner.setVisibility(View.GONE);
                when_ampm_spinner.setVisibility(View.GONE);
                when_dayofweek_spinner.setVisibility(View.GONE);
                when_date_spinner.setVisibility(View.GONE);
                when_zone_spinner.setVisibility(View.GONE);
            }
        });

        // DO selection changed listener
        do_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int pos, long id) {
                String sel = do_spinner.getSelectedItem().toString().toLowerCase(Locale.US);

                do_device_spinner.setVisibility(View.GONE);
                do_email_address.setVisibility(View.GONE);
                do_email_subject.setVisibility(View.GONE);
                do_duration_spinner.setVisibility(View.GONE);
                do_duration_units_spinner.setVisibility(View.GONE);
                do_speak_spinner.setVisibility(View.GONE);
                do_email_body.setVisibility(View.GONE);

                if (sel.contains("speak")) {
                    do_speak_spinner.setVisibility(View.VISIBLE);
                } else if (sel.contains("email")) {
                    do_email_address.setVisibility(View.VISIBLE);
                    do_email_subject.setVisibility(View.VISIBLE);
                    do_email_body.setVisibility(View.VISIBLE);
                    do_email_body.setHint("email message");
                } else if (sel.contains("device")) {
                    do_device_spinner.setVisibility(View.VISIBLE);
                    do_duration_spinner.setVisibility(View.VISIBLE);
                    do_duration_units_spinner.setVisibility(View.VISIBLE);
                    do_email_body.setHint("device");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                do_spinner.setSelection(0);
                do_device_spinner.setVisibility(View.GONE);
                do_email_address.setVisibility(View.GONE);
                do_email_subject.setVisibility(View.GONE);
                do_duration_spinner.setVisibility(View.GONE);
                do_speak_spinner.setVisibility(View.GONE);
                do_duration_units_spinner.setVisibility(View.GONE);
                do_email_body.setVisibility(View.GONE);
            }
        });

        // IF selection changed listener
        if_spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int pos, long id) {
                String sel = if_spinner1.getSelectedItem().toString().toLowerCase(Locale.US);

                if (sel.contains("zone")) {
                    if_spinner2.setVisibility(View.VISIBLE);
                } else {
                    if_spinner2.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if_spinner1.setSelection(0);
                if_spinner2.setVisibility(View.GONE);
            }
        });

        // Set focus to first item
        when_spinner.setFocusableInTouchMode(true);
        when_spinner.requestFocus();

        // Build and show the rule add dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(setupView);
        dialogBuilder.setTitle("Bungalow");
        dialogBuilder.setMessage("Rule Add");
        dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                onOkClick();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.create().show();
    }

    private void onOkClick() {

        StringBuilder sb = new StringBuilder("0000000000,");

        // WHEN, IF, DO
        String when_str = when_spinner.getSelectedItem().toString().toLowerCase(Locale.US);
        String if_str = if_spinner1.getSelectedItem().toString().toLowerCase(Locale.US);
        String do_str = do_spinner.getSelectedItem().toString().toLowerCase(Locale.US);

        // set time, if needed
        if (when_str.equals("daily") ||
                when_str.equals("weekly") ||
                when_str.equals("monthly")) {
            // Create integer hour and minute variables
            int h = Integer.parseInt(when_hour_spinner.getSelectedItem().toString());
            int m = Integer.parseInt(when_minute_spinner.getSelectedItem().toString());
            if (when_ampm_spinner.getSelectedItem().toString().equalsIgnoreCase("PM")) {
                h += 12;
            }
            if (h == 24) {
                h = 0; // midnight represented as hour=0
            }
            sb.setCharAt(2, Tools.encodeB80(h));
            sb.setCharAt(3, Tools.encodeB80(m));
        }

        if (when_str.contains("zone")) {
            String number = (when_zone_spinner.getSelectedItem().toString());
            number = number.substring(0, number.indexOf(':'));
            sb.setCharAt(1, Tools.encodeB80(Integer.parseInt(number) - 1)); // zone number
            if (when_str.contains("ready")) {
                sb.setCharAt(0, Const.WHEN_ZONE_READY);
            } else {
                sb.setCharAt(0, Const.WHEN_ZONE_FAULTED);
            }
        } else if (when_str.equals("armed")) {
            sb.setCharAt(0, Const.WHEN_ARMED);
        } else if (when_str.equals("armed stay")) {
            sb.setCharAt(0, Const.WHEN_ARMED_STAY);
        } else if (when_str.equals("armed away")) {
            sb.setCharAt(0, Const.WHEN_ARMED_AWAY);
        } else if (when_str.equals("disarmed")) {
            sb.setCharAt(0, Const.WHEN_DISARMED);
        } else if (when_str.equals("burglary")) {
            sb.setCharAt(0, Const.WHEN_BURGLARY);
        } else if (when_str.equals("fire")) {
            sb.setCharAt(0, Const.WHEN_FIRE);
        } else if (when_str.equals("daily")) {
            sb.setCharAt(0, Const.WHEN_DAILY);
        } else if (when_str.equals("weekly")) {
            sb.setCharAt(0, Const.WHEN_WEEKLY);
            sb.setCharAt(4, Tools.encodeB80(when_dayofweek_spinner.getSelectedItemPosition()));
        } else if (when_str.equals("monthly")) {
            sb.setCharAt(0, Const.WHEN_MONTHLY);
            sb.setCharAt(5, Tools.encodeB80(when_date_spinner.getSelectedItemPosition()));
        } else {
            Toast.makeText(getActivity(), "Bad rule when: " + when_str, Toast.LENGTH_LONG).show();
            return;
        }

        if (if_str.equals("always")) {
            sb.setCharAt(6, Const.IF_ALWAYS);
        } else if (if_str.contains("zone")) {
            // Extract zone number from the number/name spinner, example: "4: Front Door"
            String number = if_spinner2.getSelectedItem().toString();
            number = number.substring(0, number.indexOf(':'));
            sb.setCharAt(7, Tools.encodeB80(Integer.parseInt(number) - 1)); // zone number

            if (if_str.contains("ready")) {
                sb.setCharAt(6, Const.IF_ZONE_READY);
            } else {
                sb.setCharAt(6, Const.IF_ZONE_FAULTED);
            }
        } else if (if_str.equals("armed")) {
            sb.setCharAt(6, Const.IF_ARMED);
        } else if (if_str.equals("armed stay")) {
            sb.setCharAt(6, Const.IF_ARMED_STAY);
        } else if (if_str.equals("armed away")) {
            sb.setCharAt(6, Const.IF_ARMED_AWAY);
        } else if (if_str.equals("disarmed")) {
            sb.setCharAt(6, Const.IF_DISARMED);
        } else {
            Toast.makeText(getActivity(), "Bad rule if: " + if_str, Toast.LENGTH_LONG).show();
            return;
        }

        if (do_str.contains("email")) {
            String email_address = do_email_address.getText().toString();
            String email_subject = do_email_subject.getText().toString();
            String email_message = do_email_body.getText().toString();

            // ensure no reserved chars
            email_address = email_address.replace('~', ' ').replace('$', ' ').replace(',', ' ');
            email_subject = email_subject.replace('~', ' ').replace('$', ' ').replace(',', ' ');
            email_message = email_message.replace('~', ' ').replace('$', ' ').replace(',', ' ');
            sb.setCharAt(8, Const.DO_EMAIL);
            sb.append(email_address).append(",").append(email_subject).append(",").append(email_message);
        } else if (do_str.equals("speak")) {
            sb.setCharAt(9,Tools.encodeB80(do_speak_spinner.getSelectedItemPosition()));
            sb.append(",,");
        } else {
            Toast.makeText(getActivity(), "Bad rule do: " + do_str, Toast.LENGTH_LONG).show();
            return;
        }

        String str = sb.toString();
        //Log.d("RUL+", str);
        ((MainActivity)Objects.requireNonNull(getActivity())).send("RUL+" + str);
        Toast.makeText(getActivity(), "Added rule", Toast.LENGTH_SHORT).show();
    }

    /**
     * Builds the rule list containing readable rule descriptions
     * from the encoded rules received from the server.
     */
    static void buildRules(String packedRuleString) throws DataFormatException {
        final String AddNewRuleMsg = "[Tap to add a new rule]";
        StringBuilder ruleString = new StringBuilder();

        ruleList.clear();
        if (packedRuleString.isEmpty()) {
            ruleList.add(AddNewRuleMsg);
        }

        if (packedRuleString.equals("")) {
            return;
        }

        String[] packedRules = packedRuleString.split("~"); // split on rule boundaries
        String date = "";
        StringBuilder time = new StringBuilder();
        int whenNum, ifNum, doNum;  // Note: ints are always initialized to zero

        for (String packedRule : packedRules) {
            //Log.v("RULES", longRule);
            ruleString.setLength(0); // clear it (or could get new StringBuilder in each iteration)

            String[] parts = packedRule.split(",", -1); // -1 says don't discard trailing empty strings
            if ((parts.length != 4) || (parts[0].length() != 10)) {
                throw new DataFormatException("Bad rule length: " + packedRule);
            }

            char whenEvent = packedRule.charAt(0);
            char ifCondition = packedRule.charAt(6);
            char doTask = packedRule.charAt(8);

            // Get the time-stamp if required
            if ((whenEvent == Const.WHEN_DAILY) ||
                    (whenEvent == Const.WHEN_WEEKLY) ||
                    (whenEvent == Const.WHEN_MONTHLY)) {
                int hour = Tools.decodeB80(packedRule.charAt(2));
                int minute = Tools.decodeB80(packedRule.charAt(3));
                String ampm = "pm";
                if (hour < 12) {ampm = "am";}
                if (hour > 12) {hour -= 12;}
                if (hour == 0) {hour = 12;}
                time.setLength(0);
                time.append(hour).append(':');
                if (minute < 10) {time.append('0');}
                time.append(minute).append(' ').append(ampm);
            }

            // Get the date if required
            if (whenEvent == Const.WHEN_MONTHLY) {
                try {
                    int dt = Tools.decodeB80(packedRule.charAt(5));
                    if ((dt < 1) || (dt > 31)) {
                        throw new DataFormatException();  // will be rethrown in catch below
                    }
                    date = "" + dt;
                    if (date.endsWith("1")) {date += "st";}
                    else if (date.endsWith("2")) {date += "nd";}
                    else if (date.endsWith("3")) {date += "rd";}
                    else {date += "th";}
                } catch (Exception e) {
                    throw new DataFormatException("Bad rule date: " + packedRule);
                }
            }

            // Get the WHEN zone number (needed if: WHEN_ZONE_READY or WHEN_ZONE_FAULTED)
            whenNum = Tools.decodeB80(packedRule.charAt(1));

            // Get the IF zone number (needed if: IF_ZONE_READY or IF_ZONE_FAULTED)
            ifNum = Tools.decodeB80(packedRule.charAt(7));

            // Get the DO-device-number/speak-id (needed if: DO_DEVICE_ON or DO_DEVICE_OFF or DO_SPEAK)
            doNum = Tools.decodeB80(packedRule.charAt(9));

            try {

                // ----- WHEN -----
                switch (whenEvent) {
                    case Const.WHEN_ZONE_READY:
                        ruleString.append(String.format("When %s is ready ", ZonesFragment.getZoneName(whenNum + 1)));
                        break;
                    case Const.WHEN_ZONE_FAULTED:
                        ruleString.append(String.format("When %s is faulted ", ZonesFragment.getZoneName(whenNum + 1)));
                        break;
                    case Const.WHEN_BURGLARY:
                        ruleString.append("When burglar alarm ");
                        break;
                    case Const.WHEN_FIRE:
                        ruleString.append("When fire alarm ");
                        break;
                    case Const.WHEN_ARMED:
                        ruleString.append("When armed ");
                        break;
                    case Const.WHEN_ARMED_STAY:
                        ruleString.append("When armed-stay ");
                        break;
                    case Const.WHEN_ARMED_AWAY:
                        ruleString.append("When armed-away ");
                        break;
                    case Const.WHEN_DISARMED:
                        ruleString.append("When disarmed ");
                        break;
                    case Const.WHEN_DAILY:
                        ruleString.append(String.format("Daily at %s ", time));
                        break;
                    case Const.WHEN_WEEKLY:
                        int weekday = Tools.decodeB80(packedRule.charAt(4));
                        if ((weekday < 0) || (weekday > 6)) {
                            throw new DataFormatException("Bad rule weekday: " + packedRule); // rethrown below
                        }
                        ruleString.append(String.format("Every %s at %s ", Tools.weekDays[weekday], time));
                        break;
                    case Const.WHEN_MONTHLY:
                        ruleString.append(String.format("Every %s at %s ", date, time));
                        break;
                    default:
                        throw new DataFormatException("Bad rule when: " + packedRule); // rethrown below
                }

                // ----- IF -----
                switch (ifCondition) {
                    case Const.IF_ALWAYS:
                        break;
                    case Const.IF_ZONE_READY:
                        ruleString.append(String.format("and if %s is ready ",
                                ZonesFragment.getZoneName(ifNum + 1)));
                        break;
                    case Const.IF_ZONE_FAULTED:
                        ruleString.append(String.format("and if %s is faulted ",
                                ZonesFragment.getZoneName(ifNum + 1)));
                        break;
                    case Const.IF_ARMED:
                        ruleString.append("and if armed ");
                        break;
                    case Const.IF_ARMED_STAY:
                        ruleString.append("and if armed-stay ");
                        break;
                    case Const.IF_ARMED_AWAY:
                        ruleString.append("and if armed-away ");
                        break;
                    case Const.IF_DISARMED:
                        ruleString.append("and if disarmed ");
                        break;
                    default:
                        throw new DataFormatException("Bad rule if: " + packedRule); // rethrown below
                }

                // ----- DO -----
                switch (doTask) {
                    case Const.DO_EMAIL:
                        ruleString.append(String.format(Locale.US, "send email to %s with subject %s", parts[1], parts[2]));
                        break;
                    case Const.DO_SPEAK:
                        ruleString.append(String.format(Locale.US, "speak phrase %d", doNum));
                        break;
                    case Const.DO_TURN_ON:
                        ruleString.append(String.format(Locale.US,"turn device %d on", doNum + 1));
                        break;
                    case Const.DO_TURN_OFF:
                        ruleString.append(String.format(Locale.US,"turn device %d off", doNum + 1));
                        break;
                    default:
                        throw new DataFormatException("Bad rule do: " + packedRule); // rethrown below
                }
            }
            catch (DataFormatException e) {
                throw new DataFormatException(e.getMessage());
            }
            catch (Exception e) {
                throw new DataFormatException("Bad rule: " + packedRule);
            }

            ruleList.add(ruleString.toString());
        }

        // Add the ADD NEW RULE list item to the end of the list
        ruleList.add(AddNewRuleMsg);
    }

}

/*
 * // Enable long click and provide callback, I use this to allow edit/delete option
 * this.getListView().setLongClickable(true); this.getListView().setOnItemLongClickListener(new
 * OnItemLongClickListener() { public boolean onItemLongClick(AdapterView<?> arg0, View v, int
 * position, long id) { String item = (String) getListAdapter().getItem(position);
 * 
 * // if add new item was long pressed, return false so it calls the normal click method //if
 * (item.equals(AddNewText)) { return false; }
 * 
 * startRuleSetup(position, true);
 * 
 * //Toast.makeText(RulesActivity.this, item + " was long-pressed", Toast.LENGTH_LONG).show();
 * return true; // true says don't invoke the normal click method } });
 */
