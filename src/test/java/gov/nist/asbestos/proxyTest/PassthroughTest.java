package gov.nist.asbestos.proxyTest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PassthroughTest {

//    private FhirContext ctx;
//    private IGenericClient client;
//
//    @BeforeAll
//    void beforeAll() {
//        ctx = FhirContext.forR4();
//    }

    @Test
    void createAChannel() throws URISyntaxException, IOException {
        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("test")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir");
        String json = ChannelConfigFactory.convert(channelConfig);
        HttpPost poster = new HttpPost();
        poster.postJson(new URI("http://localhost:8081/asbestos_proxy_war_exploded/prox"), json);
        assertEquals(200, poster.getStatus());
    }
}
