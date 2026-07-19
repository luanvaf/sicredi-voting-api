package dev.luan.sicredi.voting.delivery.http;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DocumentationRedirectController {

  @GetMapping("/")
  String redirectToSwaggerUi() {
    return "redirect:/swagger-ui.html";
  }
}
