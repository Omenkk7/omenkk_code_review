package com.omenkk.sdk.types.util;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author omenkk7
 * @description jwt认证
 * @create 2025/10/15
 */
public class BearerTokenUtils {

//    c81c1cf252884618bef386852f45a467.cxL2KFmsiUlTY3Te


    // 过期时间；默认30分钟
    private static final long expireMillis = 30 * 60 * 1000L;

    // 缓存服务
    public static Cache<String, String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(expireMillis - (60 * 1000L), TimeUnit.MILLISECONDS)
            .build();

    /**
     * 获取认证token
     * @param apiKeySecret
     * @return
     */
    public static String getToken(String apiKeySecret){
        String[] split = apiKeySecret.split("//.");
        return getToken(split[0],split[1]);
    }


    public static String getToken(String apiKey,String apiSecret){


            // 缓存Token
            String token = cache.getIfPresent(apiKey);
            if (null != token) return token;
            // 创建Token
            Algorithm algorithm = Algorithm.HMAC256(apiSecret.getBytes(StandardCharsets.UTF_8));
            Map<String, Object> payload = new HashMap<>();
            payload.put("api_key", apiKey);
            payload.put("exp", System.currentTimeMillis() + expireMillis);
            payload.put("timestamp", Calendar.getInstance().getTimeInMillis());
            Map<String, Object> headerClaims = new HashMap<>();
            headerClaims.put("alg", "HS256");
            headerClaims.put("sign_type", "SIGN");
            token = JWT.create().withPayload(payload).withHeader(headerClaims).sign(algorithm);
            cache.put(apiKey, token);
            return token;


    }

}
