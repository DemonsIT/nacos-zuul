package com.alibaba.nacos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Bonree
 * @Date: 2020-4-7 14:04
 * @Desc:
 */
@RestController
@RequestMapping("upload")
public class ProviderController {
    
    @GetMapping("test")
    public String test(@RequestParam String name) {
        return "for test ".concat(name);
    }
    
    @GetMapping("first")
    public String first() {
        return "for test first empty params";
    }
    
    @GetMapping("second")
    public String second(@RequestParam Integer number) {
        return "for test second " + number;
    }
}
