package com.alibaba.nacos.console.config;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.console.utils.ConfigParseUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author bonree
 * 初始化配置文件
 */
public class ZkConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ZkConfig.class);

    /** 初始化得到网关地址 */
    private static String gatewayUrl;
    
    public ZkConfig() {
        // 从环境变量获取zk地址,从而获取网关地址
        System.out.println("start read zk info...");
        String zkAddress = System.getenv("BR_ZK_HOST");
        System.out.println("env zk address is:" + zkAddress);
        initZkParams(zkAddress);
    }
    
    /**
     * 从zk获取配置文件
     */
    private static void initZkParams(String zkAddress) {
        ZooKeeper zkHelper = null;
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            zkHelper = new ZooKeeper(zkAddress, 5000, event -> latch.countDown());
            latch.await();
            String globalPath = "/bonree/config/global";
            if (zkHelper.exists(globalPath, false) != null) {
                System.out.println(globalPath + " exist, continue...");
                String globalConfig = new String(zkHelper.getData(globalPath, false, null));
                Map<String, Object> globalConfigInfos = ConfigParseUtils.read(globalConfig, "/global/*/*");
                // 网关服务地址,多地址时取第一个
                String tmpMoreUrls = globalConfigInfos.get("gateway.url") + "";
                gatewayUrl = tmpMoreUrls.split(",")[0];
                System.out.println("gateway url is:" + gatewayUrl);
            } else {
                System.out.println(globalPath + " doesn't exist!");
            }
        } catch (Exception e) {
            System.out.println("connect to zk exception!" + e);
        } finally {
            if (zkHelper != null) {
                try {
                    zkHelper.close();
                } catch (InterruptedException e) {
                    System.out.println("Close exception!" + e);
                }
            }
        }
    }
    
    public JSONObject queryToken(String userName, String token) {
        Map<String, String> queryMap = new HashMap<>(2);
        queryMap.put("username", userName);
        queryMap.put("token", token);
        String reqUrl = gatewayUrl.concat("/checkToken");
        String response = sendPost(reqUrl, queryMap);
        return JSONObject.parseObject(response);
    }
    
    /**
     * 发送httpPost请求(带参数)
     * @param url post请求的地址
     * @param map 封装的参数信息
     * @return 字符串结果
     */
    private static String sendPost(String url, Map<String, String> map) {
        final int timeOut = 30_000;
        List<NameValuePair> formParams = new ArrayList<>();
        // 创建参数队列 (键值对列表)
        for (Map.Entry<String, String> entry : map.entrySet()) {
            formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        // 将参数设置到 entity 对象中
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8);
        HttpPost httpPost = new HttpPost(url);
        // 将entity对象设置到httppost对象中
        httpPost.setEntity(entity);
        // 使用短连接
        httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        // 设置超时时间,防止连接卡住导致程序运行异常
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeOut).setConnectionRequestTimeout(timeOut)
                .setSocketTimeout(timeOut).build();
        httpPost.setConfig(requestConfig);
        // 响应对象
        CloseableHttpResponse response = null;
        // 获取默认的client对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String result;
        try {
            // 发送请求获取响应
            response = httpClient.execute(httpPost);
            // 获取响应 entity
            result = response == null ? null: EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            logger.error("Get response exception:", e);
            throw new RuntimeException("sendPostUTF error!", e);
        } finally {
            if (response != null) {
                HttpClientUtils.closeQuietly(response);
            }
            HttpClientUtils.closeQuietly(httpClient);
        }
        return result;
    }
}
