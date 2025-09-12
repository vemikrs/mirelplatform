package jp.vemi.mirel.foundation.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * service.
     * @param model {@link Model モデル}
     * @return
     */
    @GetMapping("/home")
    public String service(Model model) {
        model.addAttribute("message", "this is home.");
        return "home";
    }
}
