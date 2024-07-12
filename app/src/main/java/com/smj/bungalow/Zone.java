package com.smj.bungalow;

class Zone {
    String Name;
    int ZoneNum;
    int Type;
    boolean Fault;
    boolean Bypass;
    final boolean AlarmMemory;
    boolean Error;

    Zone(int zone, String name, int type, boolean fault,
         boolean bypass, boolean alarm, boolean error) {
        this.Name = name;
        this.ZoneNum = zone;
        this.Fault = fault;
        this.Bypass = bypass;
        this.AlarmMemory = alarm;
        this.Error = error;
        this.Type = type;
    }
}
