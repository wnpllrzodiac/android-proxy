package com.lechucksoftware.proxy.proxysettings.utils.startup;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.R;
import com.lechucksoftware.proxy.proxysettings.constants.Constants;
import com.lechucksoftware.proxy.proxysettings.constants.StartupActionStatus;
import com.lechucksoftware.proxy.proxysettings.constants.StartupActionType;
import com.lechucksoftware.proxy.proxysettings.utils.ApplicationStatistics;
import com.lechucksoftware.proxy.proxysettings.utils.Utils;

import java.io.Serializable;

/**
 * Created by Marco on 12/04/14.
 */
public class StartupAction implements Serializable
{
    private static String keyPrefix = "STARTUP_ACTION_";
    private Activity activity;

    public String preferenceKey;
    public StartupActionType actionType;
    public StartupActionStatus actionStatus;

    public StartupCondition [] startupConditions;

    public StartupAction(Activity act, StartupActionType type, StartupActionStatus status, StartupCondition ... conditions)
    {
        activity = act;
        actionType = type;
        actionStatus = status;
        preferenceKey = keyPrefix + actionType;

        startupConditions = conditions;
    }

    public void updateStatus(StartupActionStatus status)
    {
        SharedPreferences prefs = activity.getSharedPreferences(Constants.PREFERENCES_FILENAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();

        if (editor != null)
        {
            editor.putInt(preferenceKey, status.getValue());
            editor.commit();

            App.getEventsReporter().sendEvent(activity.getString(R.string.analytics_cat_user_action), activity.getString(R.string.analytics_act_startup_action), preferenceKey, (long) status.getValue());
        }
    }

    public boolean canExecute(ApplicationStatistics statistics)
    {
        SharedPreferences prefs = activity.getSharedPreferences(Constants.PREFERENCES_FILENAME, Context.MODE_MULTI_PROCESS);
        StartupActionStatus status = StartupActionStatus.parseInt(prefs.getInt(preferenceKey, StartupActionStatus.NOT_AVAILABLE.getValue()));

        Boolean result;

        switch (status)
        {
            case NOT_AVAILABLE:
            case POSTPONED:
                result = checkInstallationConditions(statistics, startupConditions);
                break;

            case REJECTED:
            case DONE:
            case NOT_APPLICABLE:
            default:
                result = false;
        }

        return result;
    }

    public static Boolean checkInstallationConditions(ApplicationStatistics statistics, StartupCondition [] conditions)
    {
        Boolean result = false;

        if (conditions != null)
        {
            for (StartupCondition condition: conditions)
            {
                if (checkLaunchCount(statistics, condition.launchCount) &&
                    checkElapsedDays(statistics, condition.launchDays) &&
                    checkRequiredAppVersion(statistics, condition.requiredVerCode))
                {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    private static boolean checkRequiredAppVersion(ApplicationStatistics statistics, Integer requiredVerCode)
    {
        Boolean result = false;

        if (requiredVerCode == null)
        {
            result = true;
        }
        else if (App.getAppMajorVersion() == requiredVerCode)
        {
            result = true;
        }

        return result;
    }

    public static Boolean checkLaunchCount(ApplicationStatistics statistics, Integer launchCount)
    {
        Boolean result = false;

        if (launchCount == null || statistics.LaunchCount == launchCount)
        {
            result = true;
        }

        return result;
    }

    public static Boolean checkElapsedDays(ApplicationStatistics statistics, Integer daysCount)
    {
        Boolean result = false;

        if (daysCount == null || Utils.ElapsedNDays(statistics.LaunhcFirstDate, daysCount))
        {
            result = true;
        }

        return result;
    }

    @Override
    public String toString()
    {
        return String.format("%s: %s %s", preferenceKey, actionType, actionStatus);
    }
}
