import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class MockServer {
    private static final int MOCK_PORT = 8888;
    private static WireMockServer server;

    public static void start() {
        server = new WireMockServer(wireMockConfig().port(MOCK_PORT));
        server.start();

        //Мок для /auth
        server.stubFor(WireMock.post("/auth")
                .withHeader("Content-Type", WireMock.equalTo("application/x-www-form-urlencoded"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withRequestBody(WireMock.matching("token=.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\": \"OK\"}")));

        //Мок для /doAction
        server.stubFor(WireMock.post("/doAction")
                .withHeader("Content-Type", WireMock.equalTo("application/x-www-form-urlencoded"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withRequestBody(WireMock.matching("token=.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\": \"OK\"}")));
    }

    public static void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
