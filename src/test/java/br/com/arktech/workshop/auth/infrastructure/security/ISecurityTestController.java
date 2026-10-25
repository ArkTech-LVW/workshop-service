package br.com.arktech.workshop.auth.infrastructure.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/test/security")
interface ISecurityTestController {

    @GetMapping("/me")
    Long me();
}
