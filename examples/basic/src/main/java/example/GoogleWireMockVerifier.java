package example;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public final class GoogleWireMockVerifier {
    private GoogleWireMockVerifier() {
    }

    public static Result verify() throws Exception {
        WireMockServer wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/")).willReturn(ok("resolved-by-java-dns")));

            int port = wireMock.port();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://google.com:" + port + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            wireMock.verify(getRequestedFor(urlEqualTo("/"))
                    .withHeader("Host", equalTo("google.com:" + port)));

            return new Result(response.statusCode(), response.body(), port);
        } finally {
            wireMock.stop();
        }
    }

    public record Result(int statusCode, String body, int port) {
    }
}
