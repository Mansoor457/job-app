package com.mansoor.jobmanager;
import android.app.*;import android.content.*;import java.util.*;
public class ReminderScheduler {
    public static void schedule(Context c){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(c,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(c,7001,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,9);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);
        if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
    }
}
