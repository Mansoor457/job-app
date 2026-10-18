package com.mansoor.jobmanager;

public class Job {
    public long id;
    public String title="", company="", division="", location="", dateShown="", posted="", match="", resume="", resumeUri="", link="", status="TO APPLY", notes="", due="", confirmation="";
    /** True after the user changes a pipeline status; CSV imports must preserve it. */
    public boolean userStatus;
    public Job() {}
}
