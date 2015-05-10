package com.example.ntossapo.phuketprevention;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tossapon on 30/4/2558.
 */
public class Validator {
    public static boolean isTelephoneNumber(String s){
        if(s.isEmpty())
            return false;
        if(s.length() != 10)
            return false;
        if(!(s.startsWith("08") || s.startsWith("09")))
            return false;
        if(!s.matches("[0-9]+"))
            return false;
        return true;
    }


    public static boolean isIPAddress(String s){
        String IPADDRESS_PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern p = Pattern.compile(IPADDRESS_PATTERN);
        Matcher m = p.matcher(s);
        return m.matches();
    }
}
