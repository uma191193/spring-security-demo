package org.example.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoRestController {

    @GetMapping(value = "/msg")
    public String getMessage() {
        return "Welcome";
    }
}
