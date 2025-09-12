package jp.vemi.mirel.foundation.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    /**
     * service.
     * @param model {@link Model モデル}
     * @return
     */
    @GetMapping("/index")
    public String service(Model model) {
        model.addAttribute("message", "this is index.");
        return "index";
    }
}
