package com.mridang.battery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.acra.ACRA;
import org.ocpsoft.prettytime.PrettyTime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class BatteryWidget extends DashClockExtension {

	/* This is the instance of the receiver that deals with cellular status */
	private BatteryReceiver objReceiver;
	/* This is the title text of the published showing the battery status */
	private String strTitle;
	/* This is the body text of the published showing the additional details */
	private String strBody;
	/* This is the status text of the published showing the battery percent */
	private String strStatus;

	/*
	 * This class is the receiver for getting battery status events
	 */
	private class BatteryReceiver extends BroadcastReceiver {

		private List<Long> lngDischarges = new ArrayList<Long>();

		private List<Long> lngCharges = new ArrayList<Long>();
		/* This is the instance of the receiver that deals with cellular status */
		private Integer intPrevious;
		/* This is the instance of the receiver that deals with cellular status */
		private Long lngTimestamp;

		/*
		 * @see
		 * android.content.BroadcastReceiver#onReceive(android.content.Context,
		 * android.content.Intent)
		 */
		@Override
		public void onReceive(Context ctxContext, Intent ittIntent) {

			Log.d("BatteryWidget", "Received an intent about the battery state");
			SharedPreferences speSettings = PreferenceManager.getDefaultSharedPreferences(ctxContext);
			Integer intLevel = ittIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			Integer intScale = ittIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			Integer intPercent = intLevel * 100 / intScale;

			Log.d("BatteryWidget", String.valueOf(intPercent));
			Integer intStatus = ittIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			if (intStatus == BatteryManager.BATTERY_STATUS_CHARGING) {

				Log.d("BatteryWidget", "Battery is currently charging");
				Integer intLeft = 100 - intPercent;
				if (intPrevious != null && lngTimestamp != null && intPercent > intPrevious) {

					Long lngDelta = System.currentTimeMillis() - lngTimestamp;
					lngCharges.add(lngDelta);
					Log.d("BatteryWidget", String.format("\u03B4 charged in %d minutes", lngDelta / 60000));

					Long lngAverage = 0L;
					for (Long lngCharge : lngCharges) { lngAverage = lngAverage + lngCharge; }
					lngAverage = lngAverage / lngCharges.size();

					if (lngCharges.size() > speSettings.getInt("charge_points", 0)) {
						Editor ediEditor = speSettings.edit();
						ediEditor.putLong("delta_charge", lngAverage);
						ediEditor.putInt("charge_points", lngCharges.size());
						ediEditor.commit();
					}

					Long lngDuration = System.currentTimeMillis() + lngAverage * intLeft.longValue();
					String strDuration = new PrettyTime().format(new Date(lngDuration));
					strBody = getString(R.string.charged_in, strDuration);

				} else {

					if (speSettings.contains("delta_charge")) {

						Long lngDelta = speSettings.getLong("delta_charge", 0L);
						Log.d("BatteryWidget", String.format("\u03B4 previously charged in %d minutes", lngDelta / 60000));
						Long lngDuration = System.currentTimeMillis() + lngDelta * intLeft.longValue();
						String strDuration = new PrettyTime().format(new Date(lngDuration));
						strBody = getString(R.string.charged_in, strDuration);

					} else {
						strBody = getString(R.string.charged_checking);
					}

					strTitle = getString(R.string.battery_charging);

				}

			} else if (intStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {

				Log.d("BatteryWidget", "Battery is currently discharging");
				Integer intLeft = intPercent;
				if (intPrevious != null && lngTimestamp != null && intPercent < intPrevious) {

					Long lngDelta = System.currentTimeMillis() - lngTimestamp;
					lngDischarges.add(lngDelta);
					Log.d("BatteryWidget", String.format("\u03B4 discharged in %d minutes", lngDelta / 60000));

					Long lngAverage = 0L;
					for (Long lngDischarge : lngDischarges) { lngAverage = lngAverage + lngDischarge; }
					lngAverage = lngAverage / lngDischarges.size();

					if (lngDischarges.size() > speSettings.getInt("discharge_points", 0)) {
						Editor ediEditor = speSettings.edit();
						ediEditor.putLong("delta_discharge", lngAverage);
						ediEditor.putInt("discharge_points", lngDischarges.size());
						ediEditor.commit();
					}

					Long lngDuration = System.currentTimeMillis() + lngAverage * intLeft.longValue();
					String strDuration = new PrettyTime().format(new Date(lngDuration));
					strBody = getString(R.string.emptied_in, strDuration);

				} else {

					if (speSettings.contains("delta_discharge")) {

						Long lngDelta = speSettings.getLong("delta_discharge", 0L);
						Log.d("BatteryWidget", String.format("\u03B4 previously discharged in %d minutes", lngDelta / 60000));
						Long lngDuration = System.currentTimeMillis() + lngDelta * intLeft.longValue();
						String strDuration = new PrettyTime().format(new Date(lngDuration));
						strBody = getString(R.string.emptied_in, strDuration);

					} else {
						strBody = getString(R.string.emptied_checking);
					}

					strTitle = getString(R.string.battery_emptying);

				}	

			} else if (intStatus == BatteryManager.BATTERY_STATUS_FULL) {

				Log.d("BatteryWidget", "Battery is totally charged");
				strTitle = getString(R.string.battery_charged);
				strBody = getString(R.string.battery_totally);

			} else if (intStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {

				Log.d("BatteryWidget", "Battery is not charging");
				strTitle = getString(R.string.battery_nocharg);
				strBody = getString(R.string.battery_nocharg);

			} else if (intStatus == BatteryManager.BATTERY_STATUS_UNKNOWN) {

				Log.d("BatteryWidget", "Battery status is unknown");
				strTitle = getString(R.string.battery_unknown);
				strBody = getString(R.string.battery_message);

			}

			lngTimestamp = System.currentTimeMillis();
			strStatus = intPercent.toString();
			intPrevious = intPercent;
			onUpdateData(UPDATE_REASON_MANUAL);

		}

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onInitialize
	 * (boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		super.onInitialize(booReconnect);

		if (objReceiver != null) {

			try {

				Log.d("BatteryWidget", "Unregistered any existing status receivers");
				unregisterReceiver(objReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		objReceiver = new BatteryReceiver();
		registerReceiver(objReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		Log.d("BatteryWidget", "Registered the status receiver");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("BatteryWidget", "Created");
		ACRA.init(new AcraApplication(getApplicationContext()));

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d("BatteryWidget", "Checking the level of the battery");
		ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(true);

		if (strStatus != null && strBody != null && strTitle != null) {

			Log.d("BatteryWidget", String.format("Current battery level is at %s%%", strStatus));
			edtInformation.status(strStatus);
			edtInformation.expandedBody(strBody);
			edtInformation.expandedTitle(strTitle);
			edtInformation.icon(R.drawable.battery_000 + Integer.parseInt(strStatus));
			edtInformation.clickIntent(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
			edtInformation.visible(true);

		}

		try {

			if (new Random().nextInt(5) == 0 && !(0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
					Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
					String strPackage;

					for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

						strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0);

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri
								.parse("market:details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation
						.expandedBody("Thank you for using "
								+ intExtensions
								+ " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(true);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e("BatteryWidget", "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		publishUpdate(edtInformation);
		Log.d("BatteryWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();
		Log.d("BatteryWidget", "Destroyed");

	}

}