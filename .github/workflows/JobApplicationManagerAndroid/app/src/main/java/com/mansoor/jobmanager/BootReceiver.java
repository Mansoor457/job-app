package com.mansoor.jobmanager;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){ReminderScheduler.schedule(c);}
}
