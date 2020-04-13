package com.alibaba.nacos.console.filter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.console.config.ZkConfig;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Bonree
 * zuul实现鉴权
 */
@Component
public class TokenFilter extends ZuulFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenFilter.class);
    
    /** token缓存,K:token,V:用户名 */
    private static Map<String, String> tokenCache = new ConcurrentHashMap<>();
    private static final ZkConfig ZK_CONFIG = new ZkConfig();
    
    @Override
    public String filterType() {
        // pre:标准路由前过滤,即前置拦截
        return "pre";
    }
    
    @Override
    public int filterOrder() {
        return 1;
    }
    
    @Override
    public boolean shouldFilter() {
        // true:过滤器生效
        return true;
    }
    
    @Override
    public Object run() {
        // 登录校验逻辑。
        // 1）获取Zuul提供的请求上下文对象
        RequestContext ctx = RequestContext.getCurrentContext();
        // 2) 从上下文中获取request对象
        HttpServletRequest req = ctx.getRequest();
        Enumeration<String> headerNames = req.getHeaderNames();
        // 3) 从请求中获取token和用户名
        String token = req.getHeader("token");
        String userName = req.getHeader("username");
        // 4) 判断
        if (StringUtils.isBlank(token) || StringUtils.isBlank(userName) || !checkToken(userName, token)) {
            // 没有token，登录校验失败，拦截
            ctx.setSendZuulResponse(false);
            ctx.setResponseBody(responseBody(userName, token));
            // 返回错误信息
            ctx.setResponseStatusCode(HttpStatus.OK.value());
            return ctx;
        }
        // 校验通过，可以考虑把用户信息放入上下文，继续向后执行
        logger.debug("The user token:[{}] is correct, the authentication is passed, and it can be forwarded!", token);
        return null;
    }
    
    private boolean checkToken(String userName, String token) {
        // 同时校验用户名和token
        logger.info("start checkToken, user:{}, token:{}", userName, token);
        if (tokenCache.containsKey(token) && userName.equals(tokenCache.get(token))) {
            // 鉴权通过
            return true;
        } else {
            // 调接口查询
            JSONObject response = ZK_CONFIG.queryToken(userName, token);
            String errorCode = "errorCode";
            int intValue = response.getIntValue(errorCode);
            if (intValue == 0) {
                // errorCode为0,说明鉴权通过
                tokenCache.put(token, userName);
                return true;
            }
            logger.info("check token fail, reason:{}", response);
        }
        return false;
    }
    
    private String responseBody(String userName, String token) {
        JSONObject body = new JSONObject();
        body.put("code", "4001");
        body.put("userName", userName);
        body.put("token", token);
        body.put("reason", "Illegal token, authentication failed, access denied!");
        logger.debug("responseBody info:{}", body);
        return body.toJSONString();
    }
}
