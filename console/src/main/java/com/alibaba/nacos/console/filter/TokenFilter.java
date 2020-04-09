package com.alibaba.nacos.console.filter;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
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
    public Object run() throws ZuulException {
        // 登录校验逻辑。
        // 1）获取Zuul提供的请求上下文对象
        RequestContext ctx = RequestContext.getCurrentContext();
        // 2) 从上下文中获取request对象
        HttpServletRequest req = ctx.getRequest();
        Enumeration<String> headerNames = req.getHeaderNames();
        // 3) 从请求中获取token
        String token = req.getHeader("token");
        // 4) 判断
        if (StringUtils.isBlank(token)) {
            logger.warn("User token is empty, authentication fails!");
            // 没有token，登录校验失败，拦截
            ctx.setSendZuulResponse(false);
            ctx.setResponseBody(responseBody(token));
            // 返回401状态码。也可以考虑重定向到登录页
            ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            return ctx;
        }
        // 校验通过，可以考虑把用户信息放入上下文，继续向后执行
        logger.debug("The user token is correct, the authentication is passed, and it can be forwarded!");
        return null;
    }
    
    private String responseBody(String token) {
        JSONObject body = new JSONObject();
        body.put("code", "8888");
        body.put("token", token);
        body.put("reason", "Illegal token, authentication failed, access denied!");
        return body.toJSONString();
    }
}
