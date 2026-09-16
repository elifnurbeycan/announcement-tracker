package com.yasarbilgi.announcementtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "forward:/dashboard.html";
    }

    @GetMapping("/admin-login")
    public String adminLogin() {
        return "forward:/admin-login.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/admin-login.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/dashboard.html";
    }
}
