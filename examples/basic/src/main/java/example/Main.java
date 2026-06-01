package example;

public class Main {
    public static void main(String[] args) throws Exception {
        GoogleWireMockVerifier.Result result = GoogleWireMockVerifier.verify();

        System.out.println(result.statusCode());
        System.out.println(result.body());
        System.out.println("Verified google.com resolved to WireMock on 127.0.0.1:" + result.port());
    }
}
