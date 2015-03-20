package com.mridang.battery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ocpsoft.prettytime.PrettyTime;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.BatteryManager;
import android.util.Log;

import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class BatteryWidget extends ImprovedExtension {

	/* This is the list of the delta discharge times since the extension started */
	private final List<Long> lngDischarges = new ArrayList<>();
	/* This is the list of the delta charge times since the extension started */
	private final List<Long> lngCharges = new ArrayList<>();
	/* This is the instance of the receiver that deals with cellular status */
	private Integer intPrevious;
	/* This is the instance of the receiver that deals with cellular status */
	private Long lngTimestamp;

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getIntents()
	 */
	@Override
	protected IntentFilter getIntents() {
		return new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getUris()
	 */
	@Override
	protected String[] getUris() {
		return null;
	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {
		doUpdate();
	}

	/*
	 * @see com.mridang.battery.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {

		Log.d(getTag(), "Received an intent about the battery state");
		ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(false);

		Integer intLevel = ittIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		Integer intScale = ittIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		Integer intPercent = intLevel * 100 / intScale;

		Log.d(getTag(), String.format("Current battery level is %d", intPercent));
		Integer intStatus = ittIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		if (intStatus == BatteryManager.BATTERY_STATUS_CHARGING) {

			Log.d(getTag(), "Battery is currently charging");
			Integer intLeft = 100 - intPercent;
			if (intPrevious != null && lngTimestamp != null && intPercent > intPrevious) {

				Long lngDelta = System.currentTimeMillis() - lngTimestamp;
				lngCharges.add(lngDelta);
				Log.d(getTag(), String.format("\u03B4 charged in %d minutes", lngDelta / 60000));

				Long lngAverage = 0L;
				for (Long lngCharge : lngCharges) { lngAverage = lngAverage + lngCharge; }
				lngAverage = lngAverage / lngCharges.size();

				if (lngCharges.size() > getInt("charge_points", 0)) {
					Editor ediEditor = speSettings.edit();
					ediEditor.putLong("delta_charge", lngAverage);
					ediEditor.putInt("charge_points", lngCharges.size());
					ediEditor.apply();
				}

				Long lngDuration = System.currentTimeMillis() + lngAverage * intLeft.longValue();
				String strDuration = new PrettyTime().format(new Date(lngDuration));
				edtInformation.expandedBody(getString(R.string.charged_in, strDuration));

			} else {

				if (hasSetting("delta_charge")) {

					Long lngDelta = getLong("delta_charge", 0L);
					Log.d(getTag(), String.format("\u03B4 previously charged in %d minutes", lngDelta / 60000));
					Long lngDuration = System.currentTimeMillis() + lngDelta * intLeft.longValue();
					String strDuration = new PrettyTime().format(new Date(lngDuration));
					edtInformation.expandedBody(getString(R.string.charged_in, strDuration));

				} else {
					edtInformation.expandedBody(getString(R.string.charged_checking));
				}

			}

            edtInformation.expandedTitle(getString(R.string.battery_charging));

        } else if (intStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {

			Log.d(getTag(), "Battery is currently discharging");
            if (intPrevious != null && lngTimestamp != null && intPercent < intPrevious) {

				Long lngDelta = System.currentTimeMillis() - lngTimestamp;
				lngDischarges.add(lngDelta);
				Log.d(getTag(), String.format("\u03B4 discharged in %d minutes", lngDelta / 60000));

				Long lngAverage = 0L;
				for (Long lngDischarge : lngDischarges) { lngAverage = lngAverage + lngDischarge; }
				lngAverage = lngAverage / lngDischarges.size();

				if (lngDischarges.size() > getInt("discharge_points", 0)) {
					Editor ediEditor = speSettings.edit();
					ediEditor.putLong("delta_discharge", lngAverage);
					ediEditor.putInt("discharge_points", lngDischarges.size());
					ediEditor.apply();
				}

				Long lngDuration = System.currentTimeMillis() + lngAverage * intPercent.longValue();
				String strDuration = new PrettyTime().format(new Date(lngDuration));
				edtInformation.expandedBody(getString(R.string.emptied_in, strDuration));

			} else {

				if (hasSetting("delta_discharge")) {

					Long lngDelta = getLong("delta_discharge", 0L);
					Log.d(getTag(), String.format("\u03B4 previously discharged in %d minutes", lngDelta / 60000));
					Long lngDuration = System.currentTimeMillis() + lngDelta * intPercent.longValue();
					String strDuration = new PrettyTime().format(new Date(lngDuration));
					edtInformation.expandedBody(getString(R.string.emptied_in, strDuration));

				} else {
					edtInformation.expandedBody(getString(R.string.emptied_checking));
				}

			}

            edtInformation.expandedTitle(getString(R.string.battery_emptying));

        } else if (intStatus == BatteryManager.BATTERY_STATUS_FULL) {

			Log.d(getTag(), "Battery is totally charged");
			edtInformation.expandedTitle(getString(R.string.battery_charged));
			edtInformation.expandedBody(getString(R.string.battery_totally));

		} else if (intStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {

			Log.d(getTag(), "Battery is not charging");
			edtInformation.expandedTitle(getString(R.string.battery_nocharg));
			edtInformation.expandedBody(getString(R.string.battery_nocharg));

		} else if (intStatus == BatteryManager.BATTERY_STATUS_UNKNOWN) {

			Log.d(getTag(), "Battery status is unknown");
			edtInformation.expandedTitle(getString(R.string.battery_unknown));
			edtInformation.expandedBody(getString(R.string.battery_message));

		}

		lngTimestamp = System.currentTimeMillis();
		intPrevious = intPercent;

		edtInformation.icon(R.drawable.battery_000 + intPercent);
		edtInformation.clickIntent(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
		edtInformation.visible(true);
		edtInformation.status(intPercent.toString());
        doUpdate(edtInformation);

	}

}