package fr.cs.eo.utils

import org.mockserver.client.server.MockServerClient;
import org.mockserver.socket.PortFactory;

import java.net.URL;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * A base class for Spock testing using the pipeline helper
 */
class TuleapSpockTestBase extends PipelineSpockTestBase {
    protected MockServerClient mockServer;
    protected URL mockServerURL;
    protected URL creationUrl;

    public void setup() throws Exception {
        creationUrl = new URL("http://tuleap.example.com");
        int port = PortFactory.findFreePort();
        mockServerURL = new URL("http://localhost:" + port + "/");
        mockServer = startClientAndServer(port);
    }

    public void cleanup() {
        mockServer.stop();
    }
}
