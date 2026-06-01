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

public class Main {
    public static void main(String[] args) throws Exception {
        WireMockServer wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/")).willReturn(ok("resolved-by-java-dns")));

            int port = wireMock.port();
            URI uri = URI.create("http://google.com:" + port + "/");
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            wireMock.verify(getRequestedFor(urlEqualTo("/"))
                    .withHeader("Host", equalTo("google.com:" + port)));

            System.out.println(response.statusCode());
            System.out.println(response.body());
            System.out.println("Verified google.com resolved to WireMock on 127.0.0.1:" + port);
        } finally {
            wireMock.stop();
        }
    }
}
