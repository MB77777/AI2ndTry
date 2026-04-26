package eu.pp.mb.test.ai2ndtry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    private final String applicationVersion;

    HealthController(@Value("${app.version}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @GetMapping("/status")
    String status() {
        return "OK";
    }

    @GetMapping("/version")
    String version() {
        return applicationVersion;
    }
}
