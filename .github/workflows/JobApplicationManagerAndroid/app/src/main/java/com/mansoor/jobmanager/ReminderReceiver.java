package com.mansoor.jobmanager;

import android.app.*;import android.content.*;import android.os.Build;import java.util.*;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL="job_actions";
    @Override public void onReceive(Context c,Intent intent){
        createChannel(c);
        JobDb db=new JobDb(c);
        for(Job j:db.all()){
            if(!pending(j))continue;
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
            Intent open=new Intent(c,MainActivity.class);
            PendingIntent content=PendingIntent.getActivity(c,(int)(j.id+10000),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Intent applied=new Intent(c,ReminderActionReceiver.class).setAction("APPLIED").putExtra("job_id",j.id);
            Intent dismissed=new Intent(c,ReminderActionReceiver.class).setAction("DISMISS").putExtra("job_id",j.id);
            PendingIntent ap=PendingIntent.getBroadcast(c,(int)(j.id+20000),applied,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent di=PendingIntent.getBroadcast(c,(int)(j.id+30000),dismissed,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Job application reminder")
             .setContentText(j.title+" — "+j.company).setStyle(new Notification.BigTextStyle().bigText(j.title+" at "+j.company+" still needs action. Open the app to apply, or dismiss this job."))
             .setContentIntent(content).setAutoCancel(true)
             .addAction(new Notification.Action.Builder(null,"Mark applied",ap).build())
             .addAction(new Notification.Action.Builder(null,"Dismiss job",di).build());
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int)(j.id+1),b.build());
        }
    }
    static boolean pending(Job j){
        String s=(j.status==null?"":j.status).toLowerCase(Locale.ROOT);
        // Archive/reference records remain visible but should not create daily apply alerts.
        return s.contains("to apply") || s.contains("shown");
    }
    static void createChannel(Context c){
        if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Job application reminders",NotificationManager.IMPORTANCE_HIGH));}
    }
}
