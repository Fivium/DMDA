package uk.co.fivium.dmda.Server;

import java.util.regex.Pattern;

public class DomainMatcher{
    private String mDomain;
    private boolean mRegex;
    private String mDatabase;
    public DomainMatcher(String pDomain , boolean pRegex, String pDatebase){
        mDomain  = pDomain;
        mRegex = pRegex;
        mDatabase = pDatebase;
    }

    public boolean match(String pDomain){
        return mRegex ? Pattern.matches(mDomain, pDomain) : pDomain.equals(mDomain);
    }

    public String getDatabase(){
        return mDatabase;
    }
}