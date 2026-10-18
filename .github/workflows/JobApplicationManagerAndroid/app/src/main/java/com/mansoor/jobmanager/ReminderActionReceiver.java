package com.mansoor.jobmanager;
import android.content.*;import android.app.*;
public class ReminderActionReceiver extends BroadcastReceiver{
    @Override public void onReceive(Context c,Intent i){
        long id=i.getLongExtra("job_id",-1);JobDb db=new JobDb(c);Job j=db.get(id);if(j==null)return;
        if("APPLIED".equals(i.getAction()))j.status="APPLIED — awaiting confirmation";
        else if("DISMISS".equals(i.getAction()))j.status="SKIPPED / CLOSED";
        if("APPLIED".equals(i.getAction())||"DISMISS".equals(i.getAction()))j.userStatus=true;
        db.update(j);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int)(id+1));
    }
}
