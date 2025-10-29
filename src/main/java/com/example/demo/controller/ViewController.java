package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/conductor")
    public String conductorIngreso() { return "conductor_ingreso"; }

    @GetMapping("/conductor/cochera")
    public String seleccionarCochera() { return "seleccionar_cochera"; }

    @GetMapping("/conductor/mapeo")
    public String mapeoCochera() { return "mapeo_cochera"; }

    @GetMapping("/recepcion/login")
    public String recepcionLogin() { 
        // Mostrar la vista de login (pero en desarrollo no es obligatorio completar)
        return "recepcionista_login"; 
    }

    // Map "/login" a la misma vista de recepcionista para evitar 404s
    @GetMapping("/login")
    public String loginAlias() { return "recepcionista_login"; }

    @GetMapping("/recepcion/panel")
    public String recepcionPanel() { return "recepcionista_panel"; }

    @GetMapping("/recepcion/dashboard")
    public String dashboard() { return "dashboard"; }

    // Map /error to the index to avoid Spring Boot Whitelabel on missing error mapping
    @GetMapping("/error")
    public String handleError() { return "index"; }
}
