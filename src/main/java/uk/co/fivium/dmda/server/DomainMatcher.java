package uk.co.fivium.dmda.server;

import java.util.regex.Pattern;

public class DomainMatcher{
    private String mDomain;
    private boolean mRegex;
    private String mDatabase;
    private int mPriority;

    public DomainMatcher(String pDomain, boolean pRegex, String pDatebase, int pPriority){
        mDomain  = pDomain;
        mRegex = pRegex;
        mDatabase = pDatebase;
        mPriority = pPriority;
    }

    public boolean match(String pDomain){
        return mRegex ? Pattern.matches(mDomain, pDomain) : pDomain.equals(mDomain);
    }

    public String getDatabase(){
        return mDatabase;
    }

    public int getPriority() {
        return mPriority;
    }
}