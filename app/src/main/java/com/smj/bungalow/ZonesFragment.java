package com.smj.bungalow;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.DataFormatException;

public class ZonesFragment extends ListFragment {

    private ArrayAdapter<Zone> adapter = null;
    private static final ArrayList<Zone> zoneArray = new ArrayList<>(64);

    static ZonesFragment newInstance(/*index*/) {
        //Bundle args = new Bundle();
        //args.putInt("index", index);
        //fragment.setArguments(args);
        return new ZonesFragment();
    }

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }*/
	
    // Note: onActivityCreated will always be run after onCreateView
	// Called when view recreated on tab switch to rebuild the view, but only if not still in mem
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.zone_list, container, false);
        //View root = inflater.inflate(R.layout.fragment_main, container, false);
        //final TextView textView = root.findViewById(R.id.section_label);
        //pageViewModel.getText().observe(this, new Observer<String>() {
        //    @Override
        //    public void onChanged(@Nullable String s) {
        //        textView.setText(s);
        //    }
        //});
        //return root;
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
        // ZonesFragment is now visible
        // The activity might not be created yet when this is called (apparently since this
        // fragment is the initial view shown) so only update view if activity creation complete
        // if (activityCreated) {
        // Log.d("BUNGALOW", "In setMenuVisibility");
        // Toast.makeText(getActivity(), "setMenuVisibility", Toast.LENGTH_SHORT).show();
        // updateView();
        // }
    //}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ZoneAdapter(getActivity(), R.layout.zone, zoneArray);
        setListAdapter(adapter);

        // Enable long click and provide callback, to allow edit/delete options
        ListView lv = getListView();
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                onZoneItemLongClick(arg2);
                return true; // true says I handled it (don't propagate to other views)
            }
        });
    }

    // Capture long click and allow user to set the zone type
    // public void onZoneItemLongClick(AdapterView<?> av, View v, int pos, long index) {
    private void onZoneItemLongClick(int pos) {

        // Allow user to set or change the zone type of the zone
        final Zone zone = zoneArray.get(pos);
        final Spinner sp = new Spinner(getActivity());
        final ArrayAdapter<String> adp = new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                android.R.layout.simple_spinner_item, Const.ZONE_TYPE_NAMES);
        sp.setAdapter(adp);
        sp.setSelection(zone.Type);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(sp);
        builder.setTitle("Bungalow");
        builder.setMessage(zone.Name + " Zone Type");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                zone.Type = sp.getSelectedItemPosition();
                // Send the zone type back to the server here, "TYP=nt" where n/t = zone/type (B80)

                //if (netClient.isConnected()) {
                String typ = "TYP=" + Tools.encodeB80(zone.ZoneNum - 1) + Tools.encodeB80(zone.Type);
                ((MainActivity)Objects.requireNonNull(getActivity())).send(typ);
                //}
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show(); // note: same as calling builder.create().show();
    }

    void updateView() {
        // Update view if it is visible or cached (isVisible indicates the view is cached
        // which means the current view or typically one view on each side)
        if (isVisible()) adapter.notifyDataSetChanged();
        // if (isVisible()) Toast.makeText(getActivity(), "ZonesFragment.updateView", Toast.LENGTH_SHORT).show();
    }

    // These are filled in with the QRA response then used to update the info
    // whenever a QRY response is received.
    private static final String[] zoneNames = new String[64];
    private static final int[] zoneTypes = new int[64];

    /**
     * Save zone names and types to global arrays.
     * Method buildZones will use these on every update.
     * @param s Zone name and type string
     */
    static void saveZoneNamesAndTypes(String s) throws DataFormatException {
        String[] parts = s.split("~");

        // Each substring = zoneNumber, zoneType, zoneName
        for (String str : parts) {
            if (parts.length < 3)
                throw new DataFormatException("Bad zone name & type message " + str);
            int zone = Tools.decodeB80(str.charAt(0));
            int type = Tools.decodeB80(str.charAt(1));
            if ((zone < 0) || (zone > 63) || (type < 0) || (type > 9))
                throw new DataFormatException("Bad zone name & type message " + str);
            zoneTypes[zone] = type;
            zoneNames[zone] = str.substring(2);
        }
    }

    /**
     * Builds zones from string from server
     */
    static void buildZones(String s) throws DataFormatException {
        //Log.v("ZONES", s);
        String name;
        int zone, status;
        boolean fault, bypass, alarm, error;

        // must be even length
        if ((s.length() % 2) != 0) {
            throw new DataFormatException("Invalid zone string (" + s + ")");
        }

        ZonesFragment.zoneArray.clear(); // sets all array elements to null

        try {
            for (int i = 0; i < s.length(); i += 2) {

                zone = Tools.decodeB80(s.charAt(i));
                if ((zone < 0) || (zone > 63)) {
                    throw new DataFormatException("Bad zone status message " + s); // rethrown below
                }

                status = Tools.decodeB80(s.charAt(i+1));
                fault = ((status & 0x01) != 0);
                bypass = ((status & 0x02) != 0);
                alarm = ((status & 0x08) != 0);
                error = ((status & 0x10) != 0);

                // if name is empty then replace with "Zone n" else use name from array
                if ((name = zoneNames[zone]) == null) name = "Zone " + (zone+1);

                zoneArray.add(new Zone(zone+1, name, zoneTypes[zone], fault, bypass, alarm, error));
            }
        }
        catch (DataFormatException e) {
            throw new DataFormatException(e.getMessage());
        }
        catch (Exception e) {
            throw new DataFormatException("Invalid zone string (" + s + ")");
        }
    }

    /**
     * Get zone item from zone number (1..)
     * @param zoneNumber Zone number (1..)
     * @return Reference to the ZoneItem
     */
    static Zone getZone(int zoneNumber) {
        for (Zone z : zoneArray) {
            if (z.ZoneNum == zoneNumber) return z;
        }
        return null;
    }

    /**
     * Get zone name from zone number (1..)
     * @param zoneNumber Zone number (1..)
     * @return Zone name as string
     */
    static String getZoneName(int zoneNumber) {
        for (Zone z : zoneArray) {
            if (z.ZoneNum == zoneNumber) return z.Name;
        }
        return "INVALID ZONE (" + zoneNumber + ")";
    }

    /**
     * Get zone names as string array (for display in the rule spinners)
     * @return zone names as string array
     */
    static String[] getZoneNamesAsStringArray() {
        int zonecount = zoneArray.size();
        String[] zoneNames = new String[zonecount]; // Example: "4: Front Door"...
        for (int i = 0; i < zonecount; i++)
            zoneNames[i] = zoneArray.get(i).ZoneNum + ": " + zoneArray.get(i).Name;
        return zoneNames;
    }


    // Get zone type from zone number (1..)
    //public static int getZoneType(int zoneNumber) {
    //    for (Zone z : zoneArray) {
    //        if (z.ZoneNum == zoneNumber) return z.Type;
    //    }
    //    return ZoneTypeUnknown;
    //}
}
