package com.fast.radio;

public class RadioStation {
    public String name;
    public String url;
    public String country;
    public String countryCode;
    public String codec;
    public int bitrate;
    public String homepage;
    public String favicon;
    public boolean favorite;

    public RadioStation(String name, String url) { this(name,url,"","","",0,"",""); }
    public RadioStation(String name, String url, String country, String countryCode, String codec, int bitrate, String homepage, String favicon) {
        this.name=name; this.url=url; this.country=country; this.countryCode=countryCode; this.codec=codec; this.bitrate=bitrate; this.homepage=homepage; this.favicon=favicon;
    }
    @Override public String toString() { return name + (country.isEmpty()?"":"  •  "+country) + (bitrate>0?"  •  "+bitrate+" kbps":""); }
}
