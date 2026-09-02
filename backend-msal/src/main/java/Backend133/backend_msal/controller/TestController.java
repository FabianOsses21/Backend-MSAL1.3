package Backend133.backend_msal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/public")
    public String publicEndpoint() {
        return "API funcionando correctamente";
    }
}