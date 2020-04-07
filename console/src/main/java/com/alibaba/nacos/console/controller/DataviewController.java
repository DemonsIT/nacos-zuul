package com.alibaba.nacos.console.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Bonree
 * @Date: 2020-4-7 15:01
 * @Desc:
 */
@RestController
@RequestMapping("/dataview")
public class DataviewController {
    
    @GetMapping("first")
    public String test(@RequestParam Integer num) {
        return "nacos dataview test " + num;
    }
}
