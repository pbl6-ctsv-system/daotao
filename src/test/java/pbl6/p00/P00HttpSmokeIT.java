package pbl6.p00;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import pbl6.daotao.DaotaoApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DaotaoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(P00SmokeController.class)
@ActiveProfiles("test")
class P00HttpSmokeIT {
    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    @Test
    void shouldReturnJson_whenFixtureIsCalledThroughAnActualSocket() throws Exception {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("daotao");
        URI uri = URI.create("http://127.0.0.1:" + port + P00SmokeController.PATH);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build();
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            MediaType type = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
            assertThat(type.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
            assertThat(new ObjectMapper().readTree(response.body()).path("status").asText()).isEqualTo("ok");
        }
    }
}
