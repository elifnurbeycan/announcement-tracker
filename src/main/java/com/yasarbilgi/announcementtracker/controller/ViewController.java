package com.yasarbilgi.announcementtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping({
            "/login", "/login.html",
            "/admin-login", "/admin-login.html",
            "/user-login", "/user-login.html"
    })
    public String login() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/dashboard.html";
    }
}
