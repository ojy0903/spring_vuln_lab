package com.shieldus.rookies.vuln;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/vuln")
public class VulnPingController {

    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }
}
