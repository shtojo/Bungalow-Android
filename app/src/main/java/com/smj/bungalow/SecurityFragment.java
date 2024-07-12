package com.smj.bungalow;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Objects;
import java.util.zip.DataFormatException;

public class SecurityFragment extends Fragment {

    private boolean isConnected = false;  // set when updateView is called from main activity

    // Globals related to button modification and display and sec_locked icon
    private View view = null;

    // SECURITY DATA
    private static int SecurityMode = Const.SEC_UNKNOWN;
    private static boolean SystemError = false;
    private static boolean PowerIsOff = false;

    SwipeRefreshLayout swipeLayout = null;

    static SecurityFragment newInstance(/*int index*/) {
        return new SecurityFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
    // Note: onActivityCreated will always be run after onCreateView
	// Called when view recreated on tab switch to rebuild the view, but only if not still in mem
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.security, container, false);

        // Swipe down to refresh listener
        swipeLayout = view.findViewById(R.id.security_swipe);
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
        updateView(isConnected);
    }

    // setMenuVisibility is called with visible=true whenever a fragment/page is scrolled into view
    // This can be used to do processing on the fragment/page when it becomes visible to the user
    //@Override
    //public void setMenuVisibility(final boolean visible) {
    //    super.setMenuVisibility(visible);
        // if (visible) {
        // SecurityFragment is now visible
        // The activity might not be created yet when this is called (apparently since this
        // fragment is the initial view shown) so only update view if activity creation complete
        // if (activityCreated) {
        // Log.d("BUNGALOW", "In setMenuVisibility");
        // Toast.makeText(getActivity(), "setMenuVisibility", Toast.LENGTH_SHORT).show();
        // updateView();
        // }
    //}

    void updateView(boolean isConnected) {
        this.isConnected = isConnected;
        // Update view if it is visible or cached (isVisible indicates the view is cached 
        // which means the current view or typically one view on each side)
        if (!isVisible()) return;

        Objects.requireNonNull(view);
        Button btnArmStay = view.findViewById(R.id.ArmStay);
        Button btnArmAway = view.findViewById(R.id.ArmAway);
        Button btnDisarm = view.findViewById(R.id.Disarm);
        TextView securityText = view.findViewById(R.id.securityText);
        TextView securitySubtext = view.findViewById(R.id.securitySubtext);
        ImageView securityImage = view.findViewById(R.id.SecurityImage);

        // Defaults
        btnArmStay.setText(getString(R.string.secArmStay));
        btnArmStay.setVisibility(View.GONE);
        btnArmAway.setVisibility(View.GONE);
        btnDisarm.setVisibility(View.GONE);
        securitySubtext.setText("");
        securityText.setTextColor(Color.WHITE);

        // if not connected then set the text to 'not connected'   THIS IS WRONG BELOW, NEEDS TO CHECK ACTUAL CONNECTION STATE
        if (!isConnected) {
            securityImage.setImageResource(R.drawable.sec_unknown);
            securityText.setTextColor(Color.WHITE);
            securityText.setText(getString(R.string.secNotConnected));
            return;
        }

        switch (SecurityMode) {
        case Const.SEC_ARMED_STAY:
            securityImage.setImageResource(R.drawable.sec_locked);
            securityText.setText(getString(R.string.secArmedStay));
            btnArmStay.setText(getString(R.string.secInstantModeOn));
            btnArmStay.setVisibility(View.VISIBLE);
            btnDisarm.setVisibility(View.VISIBLE);
            break;
        case Const.SEC_ARMED_STAY_INSTANT:
            securityImage.setImageResource(R.drawable.sec_locked);
            securityText.setText(getString(R.string.secArmedStayInstant));
            btnArmStay.setText(getString(R.string.secInstantModeOff));
            btnArmStay.setVisibility(View.VISIBLE);
            btnDisarm.setVisibility(View.VISIBLE);
            break;
        case Const.SEC_ARMED_AWAY:
            securityImage.setImageResource(R.drawable.sec_locked);
            securityText.setText(getString(R.string.secArmedAway));
            btnDisarm.setVisibility(View.VISIBLE);
            break;
        case Const.SEC_NOT_READY:
            securityImage.setImageResource(R.drawable.sec_unlocked);
            securityText.setText(getString(R.string.secNotReadytoArm));
            break;
        case Const.SEC_READY_TO_ARM:
            securityImage.setImageResource(R.drawable.sec_unlocked);
            securityText.setTextColor(Color.GREEN);
            securityText.setText(getString(R.string.secReadytoArm));
            btnArmStay.setVisibility(View.VISIBLE);
            btnArmAway.setVisibility(View.VISIBLE);
            break;
        case Const.SEC_READY_TO_FORCE_ARM:
            securityImage.setImageResource(R.drawable.sec_unlocked);
            securityText.setTextColor(Color.YELLOW);
            securityText.setText(getString(R.string.secReadytoForceArm));
            btnArmStay.setVisibility(View.VISIBLE);
            btnArmAway.setVisibility(View.VISIBLE);
            break;
        case Const.SEC_BURGLARY:
            securityImage.setImageResource(R.drawable.sec_alert);
            securityText.setTextColor(Color.RED);
            securityText.setText(getString(R.string.secBurglary));
            btnDisarm.setVisibility(View.VISIBLE);
            break;
        case Const.SEC_FIRE:
            securityImage.setImageResource(R.drawable.sec_alert);
            securityText.setTextColor(Color.RED);
            securityText.setText(getString(R.string.secFire));
            btnArmStay.setVisibility(View.VISIBLE);
            btnArmAway.setVisibility(View.VISIBLE);
            btnDisarm.setVisibility(View.VISIBLE);
            break;
        default:
            break;
        }

        if (PowerIsOff && SystemError) {
            securitySubtext.setText(getString(R.string.secPowerOffAndSystemError));
        }
        else if (PowerIsOff) {
            securitySubtext.setText(getString(R.string.secPowerOff));
        }
        else if (SystemError) {
            securitySubtext.setText(getString(R.string.secSystemError));
        }
        securitySubtext.setTextColor(Color.RED);
    }

    /**
     * SECURITY BUILDER Process the security string and set the security data vars (SecurityMode,
     * SystemError and PowerIsOff).
     * @param s Security string from server
     */
    static void buildSecurity(String s) throws DataFormatException {
        SecurityMode = s.charAt(0);
        SystemError = (s.charAt(1) == Const.SEC2_SYSTEM_BATTERY_LOW) ||
                (s.charAt(1) == Const.SEC2_AC_PWR_OFF_AND_SYS_BAT_LOW);
        PowerIsOff = (s.charAt(1) == Const.SEC2_AC_POWER_OFF) ||
                (s.charAt(1) == Const.SEC2_AC_PWR_OFF_AND_SYS_BAT_LOW);
        if (SecurityMode == Const.SEC_UNKNOWN)
            throw new DataFormatException("Bad security mode (" + s + ")");
    }
}

/* Notes:
 * Regarding UTF byte order marks: http://www.unicode.org/unicode/faq/utf_bom.html Java does
 * not recognize UTF Byte Order Marks (BOMs). UTF-8 can be indicated with leading EF-BB-BF (UTF-16
 * with FE-FF or FF-FE), etc Therefore, the application must recognize and remove these bytes or the
 * sender must be configured to not send them. I modified the server application to not use BOMs.
 */
