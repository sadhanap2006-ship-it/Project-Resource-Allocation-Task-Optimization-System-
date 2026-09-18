package com.sist.rms.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Opening http://localhost:8080/ shows the landing page. */
@Controller
public class HomePageController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}
