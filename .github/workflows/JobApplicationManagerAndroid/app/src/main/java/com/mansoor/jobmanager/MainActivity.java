package com.mansoor.jobmanager;

import android.Manifest;import android.app.*;import android.os.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.provider.OpenableColumns;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.text.SimpleDateFormat;import java.util.*;

public class MainActivity extends Activity {
    JobDb db; LinearLayout list, root, stats; TextView summary; static final int PICK_CSV=100, PICK_RESUME=101; long resumeJobId=-1;
    @Override public void onCreate(Bundle b){super.onCreate(b);db=new JobDb(this);seedIfEmpty();ReminderReceiver.createChannel(this);ReminderScheduler.schedule(this);buildUI();render();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);}
    void seedIfEmpty(){
        if(!db.all().isEmpty())return;
        try(InputStream in=getAssets().open("seed_jobs.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
            JSONArray a=new JSONArray(out.toString("UTF-8"));
            for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Job j=new Job();j.title=o.optString("title");j.company=o.optString("company");j.division=o.optString("division");j.location=o.optString("location");j.dateShown=o.optString("date_shown");j.posted=o.optString("posted");j.match=o.optString("match");j.resume=o.optString("resume");j.link=o.optString("link");j.status=o.optString("status","TO APPLY");j.notes=o.optString("notes");db.add(j);}
        }catch(Exception e){Toast.makeText(this,"Could not load starter jobs: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void buildUI(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(32));root.setBackgroundColor(0xFFF2F6F8);sc.addView(root);setContentView(sc);
        TextView title=text("Job Action Manager",26,true,0xFF102A43);root.addView(title);
        TextView subtitle=text("Daily job alerts → action → verified application",14,false,0xFF627D98);root.addView(subtitle);
        summary=text("",15,true,0xFF243B53);summary.setPadding(0,dp(14),0,dp(12));root.addView(summary);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);root.addView(actions);
        Button imp=button("Import Arena AI CSV");imp.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_CSV);});actions.addView(imp);
        Button report=button("Share progress report");report.setOnClickListener(v->shareReport());actions.addView(report);
        Button add=button("Add a job manually");add.setOnClickListener(v->addManual());actions.addView(add);
        TextView note=text("Reminders repeat daily at 9:00 AM until you mark a job applied or dismiss it. Allow notifications when prompted. Android may delay reminders under battery restrictions.",12,false,0xFF627D98);note.setPadding(0,dp(10),0,dp(14));root.addView(note);
        TextView h=text("Your job queue",20,true,0xFF102A43);root.addView(h);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);
    }
    void render(){
        if(list==null)return;list.removeAllViews();List<Job> jobs=db.all();int pending=0,applied=0,confirmed=0,closed=0;
        for(Job j:jobs){String s=(j.status==null?"":j.status).toLowerCase(Locale.ROOT);if(ReminderReceiver.pending(j))pending++;else if(s.contains("confirmed"))confirmed++;else if(s.contains("applied"))applied++;else closed++;}
        summary.setText("TRACKER SUMMARY\n"+jobs.size()+" jobs   •   "+pending+" need action   •   "+applied+" applied / awaiting confirmation   •   "+confirmed+" confirmed");
        for(Job j:jobs){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(13),dp(12),dp(13),dp(12));card.setBackgroundColor(0xFFFFFFFF);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(10));card.setLayoutParams(cp);
            card.addView(text(j.title,17,true,0xFF102A43));card.addView(text((j.company.isEmpty()?"Company not specified":j.company)+" · "+j.location,13,true,0xFF176B87));
            card.addView(text("Status: "+j.status+(j.match.isEmpty()?"":" · Match "+j.match+"%"),12,true,statusColor(j)));
            if(!j.resume.isEmpty())card.addView(text("Resume: "+j.resume,12,false,0xFF486581));
            if(!j.notes.isEmpty())card.addView(text(j.notes.length()>260?j.notes.substring(0,260)+"…":j.notes,12,false,0xFF627D98));
            if(!j.due.isEmpty())card.addView(text("Reminder date: "+j.due,12,true,0xFF9A3412));
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            Button open=smallButton("Open job");open.setOnClickListener(v->{if(j.link!=null&&j.link.startsWith("http")){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(j.link)));}catch(Exception e){toast("Could not open job link.");}}else toast("No application link saved for this job.");});row.addView(open);
            Button applied=smallButton("Mark applied");applied.setOnClickListener(v->{j.userStatus=true;j.status="APPLIED — awaiting confirmation";if(j.due.isEmpty())j.due=afterDays(2);db.update(j);render();toast("Marked applied. Verify with a confirmation email or application ID.");});row.addView(applied);
            Button confirm=smallButton("Confirm");confirm.setOnClickListener(v->confirmJob(j));row.addView(confirm);card.addView(row);
            LinearLayout row2=new LinearLayout(this);row2.setOrientation(LinearLayout.HORIZONTAL);
            Button due=smallButton("Set reminder");due.setOnClickListener(v->chooseDate(j));row2.addView(due);
            Button resume=smallButton(j.resumeUri.isEmpty()?"Attach resume":"Open resume");resume.setOnClickListener(v->{if(!j.resumeUri.isEmpty()){try{Intent open=new Intent(Intent.ACTION_VIEW,Uri.parse(j.resumeUri));open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(open);}catch(Exception e){toast("Could not open the attached resume.");}}else{resumeJobId=j.id;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_RESUME);}});row2.addView(resume);
            Button dismiss=smallButton("Dismiss");dismiss.setOnClickListener(v->{new AlertDialog.Builder(this).setTitle("Dismiss this job?").setMessage("It will stop appearing in daily reminders.").setNegativeButton("Cancel",null).setPositiveButton("Dismiss",(d,w)->{j.status="SKIPPED / CLOSED";j.userStatus=true;db.update(j);render();}).show();});row2.addView(dismiss);card.addView(row2);
            if(!j.link.isEmpty()){TextView link=text(j.link,11,false,0xFF176B87);link.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(j.link)));}catch(Exception e){}});card.addView(link);}
            list.addView(card);
        }
    }
    int statusColor(Job j){String s=(j.status==null?"":j.status).toLowerCase(Locale.ROOT);if(s.contains("confirmed"))return 0xFF18794E;if(s.contains("applied"))return 0xFF18794E;if(ReminderReceiver.pending(j))return 0xFFA15C00;return 0xFF9B1C1C;}
    void confirmJob(Job j){
        EditText input=new EditText(this);input.setHint("Confirmation email, application ID, or note");
        new AlertDialog.Builder(this).setTitle("Verify application").setMessage("Record proof that the application was submitted.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Confirm",(d,w)->{String proof=input.getText().toString().trim();if(proof.isEmpty()){toast("Add a confirmation detail to verify it.");return;}j.confirmation=proof;j.status="APPLICATION CONFIRMED";j.userStatus=true;db.update(j);render();}).show();
    }
    void chooseDate(Job j){
        Calendar c=Calendar.getInstance();if(!j.due.isEmpty())try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(j.due));}catch(Exception e){}
        new DatePickerDialog(this,(view,y,m,day)->{j.due=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,day);db.update(j);ReminderScheduler.schedule(this);render();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }
    void addManual(){
        LinearLayout form=new LinearLayout(this);form.setPadding(dp(8),0,dp(8),0);form.setOrientation(LinearLayout.VERTICAL);
        EditText t=field("Job title"),c=field("Company"),l=field("Application URL"),r=field("Resume filename (optional)");form.addView(t);form.addView(c);form.addView(l);form.addView(r);
        new AlertDialog.Builder(this).setTitle("Add a job").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{if(t.getText().toString().trim().isEmpty()){toast("Enter a job title.");return;}Job j=new Job();j.title=t.getText().toString().trim();j.company=c.getText().toString().trim();j.link=l.getText().toString().trim();j.resume=r.getText().toString().trim();j.dateShown=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());j.status="TO APPLY";db.add(j);render();}).show();
    }
    EditText field(String hint){EditText e=new EditText(this);e.setSingleLine();e.setHint(hint);return e;}
    void shareReport(){
        List<Job> jobs=db.all();int pending=0,applied=0,confirmed=0,closed=0;StringBuilder s=new StringBuilder("JOB APPLICATION PROGRESS REPORT\nGenerated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date())).append("\n\n");
        for(Job j:jobs){String st=(j.status==null?"":j.status).toLowerCase(Locale.ROOT);if(ReminderReceiver.pending(j))pending++;else if(st.contains("confirmed"))confirmed++;else if(st.contains("applied"))applied++;else closed++;}
        s.append("Total jobs: ").append(jobs.size()).append("\nNeed action: ").append(pending).append("\nApplied, awaiting confirmation: ").append(applied).append("\nConfirmed: ").append(confirmed).append("\nClosed / dismissed: ").append(closed).append("\n\nACTION QUEUE\n");
        for(Job j:jobs)if(ReminderReceiver.pending(j))s.append("• ").append(j.title).append(" — ").append(j.company).append(j.due.isEmpty()?"":" (reminder "+j.due+")").append("\n").append(j.link).append("\n");
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"Job application progress report");i.putExtra(Intent.EXTRA_TEXT,s.toString());startActivity(Intent.createChooser(i,"Share progress report"));
    }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(req==PICK_CSV){try(InputStream in=getContentResolver().openInputStream(uri)){String txt=new String(readAll(in),StandardCharsets.UTF_8);int n=importCsv(txt);render();toast("CSV imported. "+n+" new jobs added.");}catch(Exception e){toast("Could not import CSV: "+e.getMessage());}}
        else if(req==PICK_RESUME){Job j=db.get(resumeJobId);if(j!=null){j.resumeUri=uri.toString();j.resume=getName(uri);db.update(j);try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception e){}render();toast("Resume attached.");}}
    }
    int importCsv(String txt)throws Exception{
        List<List<String>> rows=parseCsv(txt);if(rows.size()<2)throw new Exception("No job rows found");List<String> h=rows.get(0);Map<String,Integer> ix=new HashMap<>();for(int i=0;i<h.size();i++)ix.put(h.get(i).trim().toLowerCase(Locale.ROOT).replace(" ","_"),i);
        int added=0;for(int ri=1;ri<rows.size();ri++){List<String> r=rows.get(ri);if(r.isEmpty())continue;Job j=new Job();
            j.dateShown=val(r,ix,"date_shown",0);j.title=val(r,ix,"job_title",1);j.company=val(r,ix,"company",2);j.division=val(r,ix,"entity_or_division",3);j.location=val(r,ix,"location",4);j.posted=val(r,ix,"posted",5);j.match=val(r,ix,"match_pct",6);j.resume=val(r,ix,"resume_file",7);j.link=val(r,ix,"application_link",8);j.status=val(r,ix,"status",9);j.notes=val(r,ix,"notes",10);
            if(r.size()==10){
                // Arena exports use two historical 10-column layouts. Detect layout A by
                // its location in column 3; the remaining 10-column rows use layout B.
                if(isLocationValue(r.get(2))&&!isLocationValue(r.get(3))){
                    j.dateShown=r.get(0).trim();j.title=r.get(1).trim();j.location=r.get(2).trim();j.company=r.get(3).trim();j.division="";j.posted="";j.match=r.get(6).trim();j.status=r.get(7).trim();j.link=r.get(8).trim();j.resume=r.get(9).trim();j.notes=(r.get(4).trim()+(r.get(5).trim().isEmpty()?"":" | Reqs: "+r.get(5).trim())).trim();
                }else{
                    j.dateShown=r.get(0).trim();j.title=r.get(1).trim();j.company=r.get(2).trim();j.division=r.get(3).trim();j.location=r.get(4).trim();j.posted="";j.match=r.get(6).trim();j.status=r.get(7).trim();j.link=r.get(8).trim();j.resume=r.get(9).trim();j.notes=r.get(5).trim().isEmpty()?"":"Reqs/notes: "+r.get(5).trim();
                }
            }
            if(r.size()>11)j.notes=(j.notes+" "+String.join(",",r.subList(11,r.size()))).trim();
            if(j.link.isEmpty())for(String cell:r)if(cell.startsWith("https://")||cell.startsWith("http://")||cell.startsWith("mailto:")){j.link=cell;break;}
            if(j.resume.isEmpty())for(String cell:r)if(cell.contains("Mansoor_Resume_")&&(cell.endsWith(".pdf")||cell.endsWith(".docx")))j.resume=cell;
            if(j.notes.contains("Mansoor_Resume_")){for(String cell:r)if(cell.contains("Mansoor_Resume_")&&(cell.endsWith(".pdf")||cell.endsWith(".docx")))j.resume=cell;}
            if(j.resume.endsWith(".docx"))j.resume=j.resume.substring(0,j.resume.length()-5)+".pdf";
            if(j.title.isEmpty()||j.title.equalsIgnoreCase("job_title"))continue;
            Job existing=null;for(Job old:db.all())if(old.title.equalsIgnoreCase(j.title)&&old.company.equalsIgnoreCase(j.company)){existing=old;break;}
            if(existing==null){if(j.status.isEmpty())j.status="TO APPLY";db.add(j);added++;}
            else mergeImportedJob(existing,j);
        }return added;
    }
    String val(List<String> r,Map<String,Integer> ix,String key,int fallback){int i=ix.containsKey(key)?ix.get(key):fallback;return i>=0&&i<r.size()?r.get(i).trim():"";}
    boolean isLocationValue(String value){String v=value==null?"":value.trim().toLowerCase(Locale.ROOT);return v.startsWith("abu dhabi")||v.startsWith("dubai")||v.startsWith("al ain")||v.startsWith("al-ain")||v.startsWith("sharjah")||v.startsWith("ajman")||v.startsWith("ras al")||v.startsWith("umm al")||v.startsWith("fujairah")||v.startsWith("uae")||v.startsWith("u.a.e")||v.startsWith("united arab")||v.startsWith("masdar");}
    void mergeImportedJob(Job existing,Job imported){
        existing.division=imported.division;existing.location=imported.location;existing.dateShown=imported.dateShown;existing.posted=imported.posted;existing.match=imported.match;existing.resume=imported.resume;existing.link=imported.link;existing.notes=imported.notes;
        if(!existing.userStatus&&!imported.status.isEmpty())existing.status=imported.status;
        db.update(existing);
    }
    List<List<String>> parseCsv(String s){List<List<String>> out=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean q=false;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'&&q&&i+1<s.length()&&s.charAt(i+1)=='"'){cell.append('"');i++;}else if(c=='"')q=!q;else if(c==','&&!q){row.add(cell.toString());cell.setLength(0);}else if((c=='\n'||c=='\r')&&!q){if(c=='\r'&&i+1<s.length()&&s.charAt(i+1)=='\n')i++;row.add(cell.toString());cell.setLength(0);if(row.stream().anyMatch(x->!x.trim().isEmpty()))out.add(row);row=new ArrayList<>();}else cell.append(c);}
        if(cell.length()>0||!row.isEmpty()){row.add(cell.toString());out.add(row);}return out;
    }
    byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toByteArray();}
    String getName(Uri u){String result="Resume file";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)result=c.getString(i);}}catch(Exception e){}return result;}
    TextView text(String s,int size,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(0,dp(4),0,dp(4));return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(0xFFFFFFFF);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF176B87));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(p);return b;}
    Button smallButton(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setAllCaps(false);b.setPadding(dp(4),0,dp(4),0);return b;}
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    String afterDays(int days){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,days);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
