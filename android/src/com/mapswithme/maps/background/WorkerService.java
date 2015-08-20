package com.mapswithme.maps.background;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.text.TextUtils;

import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.util.LocationUtils;
import com.mapswithme.util.statistics.Statistics;

import java.util.Calendar;

public class WorkerService extends IntentService
{
  private static final String ACTION_CHECK_UPDATE = "com.mapswithme.maps.action.update";
  private static final String ACTION_DOWNLOAD_COUNTRY = "com.mapswithme.maps.action.download_country";
  private static final String ACTION_NOTIFY_PEDESTRIAN = "com.mapswithme.maps.action.notify_pedestrian";
  private static final String PREF_NOTIFICATION_ALARM = "PedestrianNotificationAlarmSet";
  private static final String PREF_NOTIFICATION_SHOWN = "PedestrianNotificationShown";

  private static final MwmApplication APP = MwmApplication.get();
  private static final SharedPreferences PREFS = MwmApplication.prefs();

  /**
   * Starts this service to check map updates available with the given parameters. If the
   * service is already performing a task this action will be queued.
   *
   * @see IntentService
   */
  public static void startActionCheckUpdate(Context context)
  {
    Intent intent = new Intent(context, WorkerService.class);
    intent.setAction(ACTION_CHECK_UPDATE);
    context.startService(intent);
  }

  /**
   * Starts this service to check if map download for current location is available. If the
   * service is already performing a task this action will be queued.
   *
   * @see IntentService
   */
  public static void startActionDownload(Context context)
  {
    final Intent intent = new Intent(context, WorkerService.class);
    intent.setAction(WorkerService.ACTION_DOWNLOAD_COUNTRY);
    context.startService(intent);
  }

  /**
   * Sets alarm to display notification about pedestrian routing available.
   */
  public static void queuePedestrianNotification()
  {
    if (isPedestrianNotificationAlarmSet())
      return;

    Calendar time = Calendar.getInstance();
    time.set(Calendar.MINUTE, 0);
    final int hour = 18;
    final int currentHour = time.get(Calendar.HOUR_OF_DAY);
    if (currentHour > hour)
      time.roll(Calendar.DAY_OF_MONTH, 1);
    time.set(Calendar.HOUR_OF_DAY, hour);

    final Intent intent = new Intent(APP, WorkerService.class);
    intent.setAction(WorkerService.ACTION_NOTIFY_PEDESTRIAN);

    final AlarmManager manager = (AlarmManager) APP.getSystemService(Context.ALARM_SERVICE);
    manager.set(AlarmManager.RTC, time.getTimeInMillis(), PendingIntent.getService(APP, 0, intent, 0));

    onPedestrianAlarmSet();
  }

  public static void checkLostPedestrianAlarm()
  {
    if (isPedestrianNotificationAlarmSet() && !isPedestrianNotificationShown())
      queuePedestrianNotification();
  }

  public WorkerService()
  {
    super("WorkerService");
  }

  @Override
  protected void onHandleIntent(Intent intent)
  {
    if (intent != null)
    {
      final String action = intent.getAction();

      switch (action)
      {
      case ACTION_CHECK_UPDATE:
        checkLostPedestrianAlarm();
        handleActionCheckUpdate();
        break;
      case ACTION_DOWNLOAD_COUNTRY:
        checkLostPedestrianAlarm();
        handleActionCheckLocation();
        break;
      case ACTION_NOTIFY_PEDESTRIAN:
        if (isPedestrianNotificationShown())
          return;
        Notifier.notifyPedestrianRouting();
        onPedestrianNotificationShown();
      }
    }
  }

  private void handleActionCheckUpdate()
  {
    if (!Framework.nativeIsDataVersionChanged())
      return;

    final String countriesToUpdate = Framework.nativeGetOutdatedCountriesString();
    if (!TextUtils.isEmpty(countriesToUpdate))
      Notifier.notifyUpdateAvailable(countriesToUpdate);
    // We are done with current version
    Framework.nativeUpdateSavedDataVersion();
  }

  private void handleActionCheckLocation()
  {
    final long delayMillis = 60000; // 60 seconds
    boolean isLocationValid = processLocation();
    Statistics.INSTANCE.trackWifiConnected(isLocationValid);
    if (!isLocationValid)
    {
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable()
      {
        @Override
        public void run()
        {
          Statistics.INSTANCE.trackWifiConnectedAfterDelay(processLocation(), delayMillis);
        }
      }, delayMillis);
    }
  }

  /**
   * Adds notification if current location isnt expired.
   *
   * @return whether notification was added
   */
  private boolean processLocation()
  {
    final LocationManager manager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
    final Location l = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    if (l != null && !LocationUtils.isExpired(l, l.getTime(), LocationUtils.LOCATION_EXPIRATION_TIME_MILLIS_LONG))
    {
      placeDownloadNotification(l);
      return true;
    }

    return false;
  }

  /**
   * Adds notification with download country suggest.
   */
  private void placeDownloadNotification(Location l)
  {
    final String country = Framework.nativeGetCountryNameIfAbsent(l.getLatitude(), l.getLongitude());
    if (!TextUtils.isEmpty(country))
    {

      final String lastNotification = PREFS.getString(country, null);
      if (lastNotification != null)
      {
        // Do not place notification if it was displayed less than 180 days ago.
        final long timeStamp = Long.valueOf(lastNotification);
        final long outdatedMillis = 180L * 24 * 60 * 60 * 1000;
        if (System.currentTimeMillis() - timeStamp < outdatedMillis)
          return;
      }

      Notifier.notifyDownloadSuggest(country, String.format(APP.getString(R.string.download_location_country), country),
          Framework.nativeGetCountryIndex(l.getLatitude(), l.getLongitude()));
      PREFS.edit().putString(country, String.valueOf(System.currentTimeMillis())).apply();
    }
  }

  private static boolean isPedestrianNotificationAlarmSet()
  {
    return PREFS.getBoolean(PREF_NOTIFICATION_ALARM, false);
  }

  private static void onPedestrianAlarmSet()
  {
    PREFS.edit().putBoolean(PREF_NOTIFICATION_ALARM, true).apply();
  }

  private static boolean isPedestrianNotificationShown()
  {
    return PREFS.getBoolean(PREF_NOTIFICATION_SHOWN, false);
  }

  private static void onPedestrianNotificationShown()
  {
    PREFS.edit().putBoolean(PREF_NOTIFICATION_SHOWN, true).apply();
  }
}
