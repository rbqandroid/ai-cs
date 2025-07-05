package com.example.customerservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web页面控制器
 * 提供前端页面访问
 */
@Controller
public class WebController {
    
    /**
     * 主页 - 智能客服聊天界面
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "智能客服系统");
        model.addAttribute("welcomeMessage", "欢迎使用智能客服系统！");
        return "index";
    }
    
    /**
     * 关于页面
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "关于系统");
        return "about";
    }
}
