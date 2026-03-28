package portifolio.deuquanto.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class Test {

    @GetMapping
    public String test(){
        return "Test ok";
    }

    @GetMapping("/login")
    public String testLogin(){
        return "Login test";
    }
}
