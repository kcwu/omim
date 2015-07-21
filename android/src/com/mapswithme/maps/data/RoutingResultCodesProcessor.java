package com.mapswithme.maps.data;

import android.content.res.Resources;
import android.util.Pair;

import com.mapswithme.maps.LocationState;
import com.mapswithme.maps.MWMApplication;
import com.mapswithme.maps.R;

import java.util.ArrayList;
import java.util.List;

// Codes correspond to native routing::IRouter::ResultCode
public class RoutingResultCodesProcessor
{
  public static final int NO_ERROR = 0;
  public static final int CANCELLED = 1;
  public static final int NO_POSITION = 2;
  public static final int INCONSISTENT_MWM_ROUTE = 3;
  public static final int ROUTING_FILE_NOT_EXIST = 4;
  public static final int START_POINT_NOT_FOUND = 5;
  public static final int END_POINT_NOT_FOUND = 6;
  public static final int DIFFERENT_MWM = 7;
  public static final int ROUTE_NOT_FOUND = 8;
  public static final int NEED_MORE_MAPS = 9;
  public static final int INTERNAL_ERROR = 10;

  public static Pair<String, String> getDialogTitleSubtitle(int errorCode)
  {
    Resources resources = MWMApplication.get().getResources();
    int titleRes = 0;
    List<String> messages = new ArrayList<>();
    switch (errorCode)
    {
    case NO_POSITION:
      if (LocationState.INSTANCE.getLocationStateMode() == LocationState.UNKNOWN_POSITION)
      {
        titleRes = R.string.dialog_routing_location_turn_on;
        messages.add(resources.getString(R.string.dialog_routing_location_unknown_turn_on));
      }
      else
      {
        titleRes = R.string.dialog_routing_check_gps;
        messages.add(resources.getString(R.string.dialog_routing_error_location_not_found));
        messages.add(resources.getString(R.string.dialog_routing_location_turn_wifi));
      }
      break;
    case INCONSISTENT_MWM_ROUTE:
    case ROUTING_FILE_NOT_EXIST:
      titleRes = R.string.routing_download_maps_along;
      messages.add(resources.getString(R.string.routing_requires_all_map));
      break;
    case START_POINT_NOT_FOUND:
      titleRes = R.string.dialog_routing_change_start;
      messages.add(resources.getString(R.string.dialog_routing_start_not_determined));
      messages.add(resources.getString(R.string.dialog_routing_select_closer_start));
      break;
    case END_POINT_NOT_FOUND:
      titleRes = R.string.dialog_routing_change_end;
      messages.add(resources.getString(R.string.dialog_routing_end_not_determined));
      messages.add(resources.getString(R.string.dialog_routing_select_closer_end));
      break;
    case DIFFERENT_MWM:
      messages.add(resources.getString(R.string.routing_failed_cross_mwm_building));
      break;
    case ROUTE_NOT_FOUND:
      titleRes = R.string.dialog_routing_unable_locate_route;
      messages.add(resources.getString(R.string.dialog_routing_cant_build_route));
      messages.add(resources.getString(R.string.dialog_routing_change_start_or_end));
      break;
    case INTERNAL_ERROR:
      titleRes = R.string.dialog_routing_system_error;
      messages.add(resources.getString(R.string.dialog_routing_application_error));
      messages.add(resources.getString(R.string.dialog_routing_try_again));
      break;
    case NEED_MORE_MAPS:
      titleRes = R.string.dialog_routing_download_and_build_cross_route;
      messages.add(resources.getString(R.string.dialog_routing_application_error));
      messages.add(resources.getString(R.string.dialog_routing_try_again));
      break;
    }

    StringBuilder builder = new StringBuilder();
    for (String messagePart : messages)
      builder.append(messagePart).append("\n\n");

    return new Pair<>(titleRes == 0 ? "" : resources.getString(titleRes), builder.toString());
  }

  public static boolean isDownloadable(int resultCode)
  {
    return resultCode == INCONSISTENT_MWM_ROUTE || resultCode == ROUTING_FILE_NOT_EXIST;
  }
}