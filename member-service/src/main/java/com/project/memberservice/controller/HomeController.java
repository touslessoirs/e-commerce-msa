package com.project.memberservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/member")
public class HomeController {

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome Member Service";
    }

    @GetMapping("/message")
    public String message(@RequestHeader("user-request") String header) {
        log.info(header);
        return "Member Request Header Message Test";
    }

    @GetMapping("/check")
    public String check(){
        return "Check Message From Member Service";
    }
}
