package eu.pp.mb.test.ai2ndtry;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class FrontendController {

    @GetMapping("/sailbank")
    String sailbank() {
        return "redirect:/sailbank/index.html";
    }
}
