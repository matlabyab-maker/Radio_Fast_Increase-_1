package com.fast.radio;

import java.util.*;

public final class RegionCatalog {
    private RegionCatalog(){}
    public static final String[] REGIONS={"America","Europe","Africa","Asia","National"};
    private static final Map<String,String> R=new HashMap<>();
    static{
        add("America","US CA MX BR AR CL CO PE VE UY PY BO EC CR PA GT HN SV NI BZ CU DO HT JM BS TT BB GD LC VC AG DM KN GY SR GF");
        add("Europe","GB IE FR DE ES PT IT CH AT BE NL LU DK NO SE FI IS PL CZ SK HU RO BG GR AL HR SI BA RS ME MK XK EE LV LT UA MD BY RU MT CY TR AD MC SM VA LI LU");
        add("Africa","EG MA DZ TN LY SD SS ET ER DJ SO KE UG TZ RW BI CD CG GA CM CF TD NE NG GH CI SN GM GN SL LR BF ML MR CV GW GQ ST AO NA BW ZM ZW MZ MW SZ LS ZA") ;
        add("Asia","IR IQ SA AE IL JO LB SY TR YE OM KW QA BH PK AF IN BD LK NP BT MV MM TH VN LA KH MY SG ID PH CN JP KR KP MN KZ UZ TM TJ KG AZ AM GE") ;
    }
    private static void add(String region,String codes){for(String c:codes.split(" "))R.put(c,region);}
    public static String region(String code){if(code==null)return "National"; String x=R.get(code.toUpperCase()); return x==null?"National":x;}
}
