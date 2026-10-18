package com.mansoor.jobmanager;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

public class JobDb extends SQLiteOpenHelper {
    public JobDb(Context c){super(c,"job_action_manager.db",null,2);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE jobs(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,company TEXT,division TEXT,location TEXT,date_shown TEXT,posted TEXT,match_pct TEXT,resume TEXT,resume_uri TEXT,link TEXT,status TEXT,notes TEXT,due TEXT,confirmation TEXT,status_user INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){if(oldV<2)db.execSQL("ALTER TABLE jobs ADD COLUMN status_user INTEGER NOT NULL DEFAULT 0");}
    public long add(Job j){
        SQLiteDatabase db=getWritableDatabase(); ContentValues v=values(j);
        return db.insert("jobs",null,v);
    }
    public void update(Job j){getWritableDatabase().update("jobs",values(j),"id=?",new String[]{String.valueOf(j.id)});}
    public void deleteAll(){getWritableDatabase().delete("jobs",null,null);}
    public List<Job> all(){
        List<Job> out=new ArrayList<>(); Cursor c=getReadableDatabase().rawQuery("SELECT * FROM jobs ORDER BY id DESC",null);
        try{while(c.moveToNext()){Job j=new Job();j.id=c.getLong(c.getColumnIndexOrThrow("id"));j.title=s(c,"title");j.company=s(c,"company");j.division=s(c,"division");j.location=s(c,"location");j.dateShown=s(c,"date_shown");j.posted=s(c,"posted");j.match=s(c,"match_pct");j.resume=s(c,"resume");j.resumeUri=s(c,"resume_uri");j.link=s(c,"link");j.status=s(c,"status");j.notes=s(c,"notes");j.due=s(c,"due");j.confirmation=s(c,"confirmation");j.userStatus=c.getInt(c.getColumnIndexOrThrow("status_user"))!=0;out.add(j);}}finally{c.close();}return out;
    }
    public Job get(long id){for(Job j:all())if(j.id==id)return j;return null;}
    private String s(Cursor c,String k){String x=c.getString(c.getColumnIndexOrThrow(k));return x==null?"":x;}
    private ContentValues values(Job j){
        ContentValues v=new ContentValues();v.put("title",j.title);v.put("company",j.company);v.put("division",j.division);v.put("location",j.location);v.put("date_shown",j.dateShown);v.put("posted",j.posted);v.put("match_pct",j.match);v.put("resume",j.resume);v.put("resume_uri",j.resumeUri);v.put("link",j.link);v.put("status",j.status);v.put("notes",j.notes);v.put("due",j.due);v.put("confirmation",j.confirmation);v.put("status_user",j.userStatus?1:0);return v;
    }
}
