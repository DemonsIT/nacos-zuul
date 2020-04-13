package com.alibaba.nacos.console.filter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.console.service.TokenService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author: Bonree
 * zuul实现鉴权
 */
@Component
public class TokenFilter extends ZuulFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenFilter.class);
    
    private static final TokenService TOKEN_SERVICE = new TokenService();
    
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
        if (StringUtils.isBlank(token) || StringUtils.isBlank(userName) || !TOKEN_SERVICE.checkToken(userName, token)) {
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
    
    private String responseBody(String userName, String token) {
        JSONObject body = new JSONObject();
        body.put("code", "4001");
        body.put("userName", userName);
        body.put("token", token);
        body.put("reason", "Illegal username or token, authentication failed, access denied!");
        logger.debug("responseBody info:{}", body);
        return body.toJSONString();
    }
}
