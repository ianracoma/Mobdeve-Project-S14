package com.mobdeve.s14.group20.mobdeveproject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Note {

    private String title, subtitle, noteType, dateModified;
    private ArrayList<String> tags;

    public Note(String title, String subtitle, String noteType, String dateModified, ArrayList<String> tags) {
        this.title = title;
        this.subtitle = subtitle;
        this.noteType = noteType;
        this.dateModified = dateModified;
        this.tags = tags;
    }

    public String getTitle(){ return this.title; }

    public String getSubtitle(){ return this.subtitle; }

    public String getNoteType(){ return this.noteType; }

    public String getDateModified(){ return this.dateModified; }

    public ArrayList<String> getTags(){ return this.tags; }

    public void setTitle(String title){ this.title = title; }

    public void setSubtitle(String subtitle){ this.subtitle = subtitle; }

    public void setDateModified(String dateModified){ this.dateModified = dateModified; }

    public void addTag(String tag){ tags.add(tag); }
}