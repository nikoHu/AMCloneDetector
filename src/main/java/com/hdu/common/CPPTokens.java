package com.hdu.common;

import java.util.HashMap;
import java.util.Map;

public class CPPTokens extends CTokens {

    public static final byte NAMESPACE = 93;
    public static final byte CLASS = 94;

    public static Map<String, Byte> tokenMap = new HashMap<>();

    static {
        tokenMap.putAll(CTokens.tokenMap);
        tokenMap.put("class", CLASS);
        tokenMap.put("namespace", NAMESPACE);
    }
}
