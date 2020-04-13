package com.alibaba.nacos.console.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.console.config.ZkConfig;
import com.alibaba.nacos.console.utils.Tuple2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: Bonree
 * @Date: 2020-4-13 14:23
 * @Desc:
 */
public class TokenService {
    /** token缓存,K:token,V:用户名-时间 */
    private static final Map<String, Tuple2<String, Long>> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final ZkConfig ZK_CONFIG = new ZkConfig();
    
    public TokenService() {
        updateToken();
    }
    
    public boolean checkToken(String userName, String token) {
        // 命中缓存,鉴权通过
        if (TOKEN_CACHE.containsKey(token)) return true;
        // 调接口查询
        JSONObject response = ZK_CONFIG.queryToken(userName, token);
        String errorCode = "errorCode";
        int intValue = response.getIntValue(errorCode);
        if (intValue == 0) {
            // errorCode为0,说明鉴权通过
            TOKEN_CACHE.put(token, new Tuple2<>(userName, System.currentTimeMillis()));
            System.out.println("token cache:" + TOKEN_CACHE);
            return true;
        } else {
            System.out.println("auth fail, reason:" + response);
        }
        return false;
    }
    
    /**
     * 定时更新token缓存,三秒则过期,需清理
     */
    private void updateToken() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            if (TOKEN_CACHE.isEmpty()) return;
            Set<String> tmpInvalidToken = new HashSet<>();
            for (Map.Entry<String, Tuple2<String, Long>> entry :
                    TOKEN_CACHE.entrySet()) {
                Long createTime = entry.getValue().getRight();
                long currentTime = System.currentTimeMillis();
                if ((currentTime - createTime) >= 3_000) {
                    tmpInvalidToken.add(entry.getKey());
                }
            }
            if (!tmpInvalidToken.isEmpty()) {
                System.out.println("need to remove token:" + tmpInvalidToken);
                for (String invalidToken : tmpInvalidToken) {
                    TOKEN_CACHE.remove(invalidToken);
                }
            }
        }, 5, 1, TimeUnit.SECONDS);
    }
}
