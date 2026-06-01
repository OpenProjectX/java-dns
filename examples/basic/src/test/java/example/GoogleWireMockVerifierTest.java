package example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleWireMockVerifierTest {
    @Test
    void resolvesGoogleComToWireMock() throws Exception {
        GoogleWireMockVerifier.Result result = GoogleWireMockVerifier.verify();

        assertEquals(200, result.statusCode());
        assertEquals("resolved-by-java-dns", result.body());
    }
}
