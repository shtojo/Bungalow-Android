package com.smj.bungalow;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.DataFormatException;

public class EventsFragment extends ListFragment {

    private EventAdapter adapter = null;

    private static ArrayList<Event> eventLogData = new ArrayList<>();
    private ListView lv = null;

    SwipeRefreshLayout swipeLayout = null;

    static EventsFragment newInstance() {
        //Bundle bundle = new Bundle();
        //bundle.putInt(ARG_SECTION_NUMBER, index);
        //fragment.setArguments(bundle);
        return new EventsFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.event_list, container, false);

        // Swipe down to refresh listener
        swipeLayout = view.findViewById(R.id.event_swipe);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((MainActivity)Objects.requireNonNull(getActivity())).send("QRY");
                // MainActivity NetworkTask onPostExecute will call
                // swipeLayout.setRefreshing(false) to stop the refresh animation
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        swipeLayout = null;
    }

    // onResume is called when a fragment/page is loaded.
    // Typically the current page is cached as well as one page on each side.
    // So this is not called when a page comes into view, but when it is loaded to memory.
    @Override
    public void onResume() {
        super.onResume();
        //Toast.makeText(getActivity(), "EventsFragment.onResume update", Toast.LENGTH_SHORT).show();
        updateView();
    }

    // setMenuVisibility is called with visible=true whenever a fragment/page is scrolled into view
    // This can be used to do processing on the fragment/page when it becomes visible to the user
    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
            if (lv != null) lv.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new EventAdapter(getActivity(), R.layout.event, eventLogData);
        setListAdapter(adapter);
        
        // save the listview for use in setMenuVisiblity scrolling
        // can't get the list view in that method because the view may not exist yet
        // so here I know the view exists so change lv from null to the actual listview
        lv = getListView();  
    }

    void updateView() {
        // Update view if it is visible or cached (isVisible indicates the view is cached 
        // which means the current view or typically one view on each side)
        if (isVisible()) {
            adapter.notifyDataSetChanged();
            getListView().smoothScrollToPosition(0);
        }
    }

    /**
     * Adapter
     * Note: Can extend BaseAdapter and access my own arrays or data structures to fill-in the rows
     * or can extend ArrayAdapter if my data is stored in an array then some of the methods are
     * handled Base Adapter as the name suggests, is a base class for all the adapters. When you are
     * extending the Base adapter class you need to implement all methods like getcount,getid etc.
     * ArrayAdapter is a class which can work with array of data. you need to override only getview
     * method
     */
    private class EventAdapter extends ArrayAdapter<Event> {

        // Constructor
        @SuppressWarnings("SameParameterValue")
        EventAdapter(Context context, int textViewResourceId, ArrayList<Event> events) {
            super(context, textViewResourceId, events);
            //this.context = context;  this.events = events;
            //this.mInflater = LayoutInflater.from(context);
            //mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                // Use ViewHolder pattern for smooth scrolling.  This block runs only the
                // first time each row is created.  The row data reference is saved for smooth
                // scrolling by not invoking findViewById and inflating on every list scroll

                // Can do it this way (get this activity context then get layout inflater)
                LayoutInflater inflater = (LayoutInflater) Objects.requireNonNull(getActivity()).getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);

                convertView = Objects.requireNonNull(inflater).inflate(R.layout.event, parent, false);
                //convertView = inflater.inflate(R.layout.event, parent, false);

                // Note: using saved context
                //LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                // Note: using saved inflater
                //convertView = mInflater.inflate(R.layout.event, null);

                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.eventIcon);
                holder.time = convertView.findViewById(R.id.eventTime);
                holder.desc = convertView.findViewById(R.id.eventDesc);
                convertView.setTag(holder); // save for later look-up
            }
            else {
                // view already exists, get the holder instance from the view
                holder = (ViewHolder) convertView.getTag();
            }

            Event event = getItem(position);
            if (event != null) {
                holder.icon.setImageResource(event.iconId);
                holder.time.setText(event.timeStamp);
                holder.desc.setText(event.eventDescription);
            }
            return convertView;
        }
    }

    private static void AddEvent(String time, String description, int type) {
        Event event = new Event();
        event.timeStamp = time;
        event.eventDescription = description;
        event.iconId = type;
        eventLogData.add(event);
        //eventLogData.add(new Event(time, description, type));
    }

    private static class Event {
        String timeStamp;
        String eventDescription;
        int iconId; // The R.drawable of the icon to display for this event
    }

    private class ViewHolder {
        ImageView icon;
        TextView time;
        TextView desc;
    }

    /**
     * EVENT BUILDER: Builds the event strings for the event log display
     * @param s Event string from server
     */
    static void buildEvents(String s) throws DataFormatException {
        int repeat, zoneNum;
        int hour, minute, second;
        String ampm;
        int event;
        Zone zone;
        int icon;
        StringBuilder time = new StringBuilder();
        StringBuilder evnt = new StringBuilder();

        eventLogData.clear();
        int len = s.length();
        if (len == 0) return; // no logs stored

        // Validate length = 9
        if (len % 9 != 0) {
            throw new DataFormatException("Bad event string (" + s + ")");
        }

        try {
            for (int i = 0; i < len; i += 9) {
                // Keep the original StringBuilders (don't create new each loop)
                // But set the length to zero (keeps the same capacity as previous loop)
                time.setLength(0);
                evnt.setLength(0);
                time.append(Tools.weekDays[Tools.decodeB80(s.charAt(i))]).append(' '); // weekday
                time.append(Tools.months[Tools.decodeB80(s.charAt(i + 1)) - 1]).append(' '); // month
                time.append(Tools.decodeB80(s.charAt(i + 2))).append(", "); // date
                hour = Tools.decodeB80(s.charAt(i + 3));
                minute = Tools.decodeB80(s.charAt(i + 4));
                second = Tools.decodeB80(s.charAt(i + 5));

                if (hour < 12) {ampm = "am";} else {ampm = "pm";}
                if (hour > 12) hour -= 12;
                if (hour == 0) hour = 12;

                time.append(hour).append(':');
                if (minute < 10) time.append('0');
                time.append(minute).append(':');
                if (second < 10) time.append('0');
                time.append(second).append(' ').append(ampm).append(' ');

                event = Tools.decodeB80(s.charAt(i + 6));
                zoneNum = Tools.decodeB80(s.charAt(i + 7)) + 1;
                repeat = Tools.decodeB80(s.charAt(i + 8)) + 1;

                switch (event) {

                    case Const.EVENT_ZONE_READY:  // Zone ready
                        zone = ZonesFragment.getZone(zoneNum);
                        if (zone == null) {
                            throw new DataFormatException("Bad zone: " + zoneNum);  // rethrown below
                        }
                        icon = R.drawable.ready;
                        if ((zone.Type == Const.ZONE_TYPE_DOOR) ||
                                (zone.Type == Const.ZONE_TYPE_WINDOW))
                            evnt.append(zone.Name).append(" closed");
                        else evnt.append(zone.Name).append(" ready");
                        break;

                    case Const.EVENT_ZONE_FAULTED:  // Zone faulted
                        zone = ZonesFragment.getZone(zoneNum);
                        if (zone == null) {
                            throw new DataFormatException("Bad zone: " + zoneNum);  // rethrown below
                        }
                        switch (zone.Type) {
                            case Const.ZONE_TYPE_MOTION :
                                icon = R.drawable.motion;
                                break;
                            case Const.ZONE_TYPE_GLASS :
                                icon = R.drawable.glass;
                                break;
                            case Const.ZONE_TYPE_INFO :
                                icon = R.drawable.info;
                                break;
                            case Const.ZONE_TYPE_FREEZE :
                                icon = R.drawable.freeze;
                                break;
                            case Const.ZONE_TYPE_UNKNOWN :
                                icon = R.drawable.unknown;
                                break;
                            default:
                                icon = R.drawable.fault;
                                break;
                        }

                        evnt.append(zone.Name);
                        if ((zone.Type == Const.ZONE_TYPE_DOOR) ||
                                (zone.Type == Const.ZONE_TYPE_WINDOW))
                            evnt.append(" opened");
                        //else evnt.append(" faulted");
                        break;

                    case Const.EVENT_ZONE_ERROR:  // Zone error (tamper/trouble/lost/lowbatt)
                        zone = ZonesFragment.getZone(zoneNum);
                        if (zone == null) {
                            throw new DataFormatException("Bad zone: " + zoneNum);  // rethrown below
                        }
                        icon = R.drawable.error;
                        evnt.append(zone.Name).append(" error! (trouble/batt/tamper/lost)");
                        break;

                    case Const.EVENT_ZONE_ERROR_CLEARED:  // Zone error cleared
                        zone = ZonesFragment.getZone(zoneNum);
                        if (zone == null) {
                            throw new DataFormatException("Bad zone: " + zoneNum);  // rethrown below
                        }
                        icon = R.drawable.errclear;
                        evnt.append(zone.Name).append(" error cleared");
                        break;

                    case Const.EVENT_ZONE_FORCE_ARMED:  // Zone force-armed
                        zone = ZonesFragment.getZone(zoneNum);
                        if (zone == null) {
                            throw new DataFormatException("Bad zone: " + zoneNum);  // rethrown below
                        }
                        icon = R.drawable.armed;
                        evnt.append(zone.Name).append(" force-armed");
                        break;

                    case Const.EVENT_ARMED:
                        icon = R.drawable.armed;
                        evnt.append("System armed");
                        break;

                    case Const.EVENT_ARMED_STAY:
                        icon = R.drawable.armed;
                        evnt.append("System armed stay");
                        break;

                    case Const.EVENT_DISARMED:
                        icon = R.drawable.disarmed;
                        evnt.append("System disarmed");
                        break;

                    case Const.EVENT_INSTANT_MODE_ON:
                        icon = R.drawable.armed;
                        evnt.append("Instant mode on");
                        break;

                    case Const.EVENT_INSTANT_MODE_OFF:
                        icon = R.drawable.armed;
                        evnt.append("Instant mode off");
                        break;

                    case Const.EVENT_FIRE_ALARM:
                        icon = R.drawable.fire;
                        evnt.append("FIRE!");
                        break;

                    case Const.EVENT_BURGLARY_ALARM:
                        icon = R.drawable.burglary;
                        evnt.append("BURGLARY!");
                        break;

                    case Const.EVENT_ALARM_OFF:
                        icon = R.drawable.sirenoff;
                        evnt.append("Siren off");
                        break;

                    case Const.EVENT_TIMING_ENTRY:
                        icon = R.drawable.timing;
                        evnt.append("Timing entry");
                        break;

                    case Const.EVENT_SMOKE_RESET:
                        icon = R.drawable.smoke_rst;
                        evnt.append("Smoke detectors reset");
                        break;

                    case Const.EVENT_SENSOR_LOST:
                        icon = R.drawable.lost;
                        evnt.append("Sensor lost or low battery");
                        break;

                    case Const.EVENT_SYSTEM_BATTERY_LOW:
                        icon = R.drawable.batt_low;
                        evnt.append("System battery is low");
                        break;

                    case Const.EVENT_SYSTEM_BATTERY_OK:
                        icon = R.drawable.batt_ok;
                        evnt.append("System battery is ok");
                        break;

                    case Const.EVENT_AC_POWER_FAIL:
                        icon = R.drawable.ac_off;
                        evnt.append("AC power fail");
                        break;

                    case Const.EVENT_AC_POWER_RESTORED:
                        icon = R.drawable.ac_on;
                        evnt.append("AC power restored");
                        break;

                    case Const.EVENT_GLASS_BREAK:
                        icon = R.drawable.glass;
                        evnt.append("Glass break!");
                        break;

                    case Const.EVENT_DOORBELL:
                        icon = R.drawable.info;
                        evnt.append("Doorbell");
                        break;

                    case Const.EVENT_FREEZE:
                        icon = R.drawable.freeze;
                        evnt.append("FREEZING TEMPS!");
                        break;

                    case Const.EVENT_APPLICATION_STARTED:
                        icon = R.drawable.power;
                        evnt.append("- Application started -");
                        break;

                    default:
                        icon = R.drawable.unknown;
                        evnt.append("UNKNOWN EVENT: ").append(event);
                }

                // Get repeat count and append count if not 1st event, 0 = 1st occurrence, 1 = "x2", 2 = "x3"
                // Note: when repeat count reaches 80, a new event will be created if same event occurs again
                if (repeat > 1) {
                    evnt.append(String.format(Locale.US, " (x%d)", repeat));
                }
                AddEvent(time.toString(), evnt.toString(), icon);
            }
        }
        catch (DataFormatException e) {
            throw new DataFormatException(e.getMessage());
        }
        catch (Exception e) {
            throw new DataFormatException("Event error after " + time.toString() + " in" + s);
        }
    }
}