package org.example.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/producer")
public class DemoRestController {

    @GetMapping(value = "/message")
    public String getMessage() {
        return "Welcome Uma!!";
    }

    @GetMapping(value = "/contact")
    public String getContact() {
        return "7382046653";
    }
}
