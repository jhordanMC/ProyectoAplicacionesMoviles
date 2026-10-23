package com.example.saasvilcas.feature.profile.domain;

import java.util.List;
public final class Announcement {
    public final String id,title,message,image,expires,votedOption;
    public final boolean seen,satisfaction;
    public final List<Option> options;
    public Announcement(String id,String title,String message,String image,String expires,String votedOption,boolean seen,boolean satisfaction,List<Option> options) {
        this.id=id; this.title=title; this.message=message; this.image=image; this.expires=expires;
        this.votedOption=votedOption; this.seen=seen; this.satisfaction=satisfaction; this.options=java.util.Collections.unmodifiableList(options);
    }
    public static final class Option {
        public final String id,text; public final Integer votes;
        public Option(String id,String text,Integer votes) { this.id=id; this.text=text; this.votes=votes; }
    }
}
