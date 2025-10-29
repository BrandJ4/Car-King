package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/vista")
    public String index() {
        return "index";
    }

    @GetMapping("/conductor/ingreso")
    public String conductor() {
        return "conductor_ingreso";
    }

    @GetMapping("/recepcionista")
    public String recepcionista() {
        return "recepcionista_login";
    }
}
