package com.hmdp.utils;

import java.util.concurrent.TimeUnit;

public class RedisConstans {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final long LOGIN_CODE_EXPIRE = 2L;

    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final long LOGIN_TOKEN_EXPIRE = 30L;

}
