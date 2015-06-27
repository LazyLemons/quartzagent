package com.keremc.quartzagent;

public enum Status {

    OK       ( 0 ),
    WARNING  ( 1 ),
    CRITICAL ( 2 ),
    UNKNOWN  ( 3 );

    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
