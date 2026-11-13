package pbl6.p00;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class P00SmokeController {
    static final String PATH = "/__p00/smoke";

    @GetMapping(PATH)
    Map<String, String> smoke() {
        return Map.of("status", "ok");
    }
}
