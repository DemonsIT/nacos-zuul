package com.alibaba.nacos.console.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bonree
 * 解析xml文件
 */
public class ConfigParseUtils {
    
    public static Map<String, Object> read(String filePath, String xpath) {
        Map<String, List<String>> map = getConfig(filePath, xpath);
        Joiner join = Joiner.on(",");
        return map.entrySet().stream().filter(entry -> entry.getKey() != null
                && entry.getKey().length() != 0)
                .collect(Collectors.toMap(Map.Entry<String, List<String>>::getKey,
                        entry -> join.join(entry.getValue())));
    }

    private static Map<String, List<String>> getConfig(String config, String nodeName) {
        Map<String, List<String>> configs = Maps.newHashMap();
        try {
            // 创建DOM解析器工厂
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            // 获取DOM解析器对象
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputStream in = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));
            Document document = builder.parse(in);
            // XPath 是一门在 xml 文档中查找信息的语言。
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xpath.evaluate(nodeName, document, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                NodeList childs = node.getChildNodes();
                String key;
                if (!node.hasChildNodes()) {
                    key = node.getAttributes().getNamedItem("name").getNodeValue();
                    String var18 = node.getAttributes().getNamedItem("value").getNodeValue();
                    if (configs.containsKey(key)) {
                        configs.get(key).add(var18);
                    } else {
                        List<String> var19 = Lists.newArrayList();
                        var19.add(var18);
                        configs.put(key, var19);
                    }
                } else {
                    Node nameNode = node.getAttributes().getNamedItem("name");
                    if (nameNode == null) {
                        continue;
                    }
                    key = nameNode.getNodeValue();
                    for (int value = 0; value < childs.getLength(); ++value) {
                        Node arrayList = childs.item(value);
                        if (arrayList.getNodeType() == 1) {
                            NodeList values = arrayList.getChildNodes();
                            for (int j = 0; j < values.getLength(); ++j) {
                                Node value1 = values.item(j);
                                if (value1.getNodeType() == 1) {
                                    String theValue = value1.getTextContent();
                                    if (configs.containsKey(key)) {
                                        configs.get(key).add(theValue);
                                    } else {
                                        List<String> arrayList1 = Lists.newArrayList();
                                        arrayList1.add(theValue);
                                        configs.put(key, arrayList1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception var17) {
            var17.printStackTrace();
        }
        return configs;
    }
}
