package eu.pp.mb.test.ai2ndtry;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    @GetMapping("/status")
    String status() {
        return "OK";
    }
}
