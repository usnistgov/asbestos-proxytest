package gov.nist.asbestos.hapitest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor
import gov.nist.asbestos.simapi.http.HttpBase
import gov.nist.asbestos.simapi.http.HttpDelete
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.http.HttpPost
import gov.nist.asbestos.simapi.http.ParameterBuilder
import gov.nist.asbestos.simapi.sim.basic.EventStoreItem
import gov.nist.asbestos.simapi.sim.basic.EventStoreItemFactory
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.hl7.fhir.dstu3.model.Bundle
import org.hl7.fhir.dstu3.model.Patient
import org.hl7.fhir.instance.model.api.IIdType
import spock.lang.Shared
import spock.lang.Specification

class HapiTest extends Specification {
    @Shared FhirContext ctx = FhirContext.forDstu3()
    IGenericClient client

    def 'fhir create patient through proxy' () {
        setup:  // create channel
        deleteChannels()
        withBase("http://localhost:8081/fproxy_war/prox/${createChannel('default', 'fhirpass')}/Channel")

        when: // submit patient resource
        Patient patient = new Patient();
        // ..populate the patient object..
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        def location = createPatient(patient)

        then:  // if successful then id assigned
        location

        when: // pull logs
        HttpGet getter = new HttpGet()
        getter.getJson("http://localhost:8081/fproxy_war/prox/default__fhirpass/Event")

        then:
        getter.status == 200
        List<EventStoreItem> items = EventStoreItemFactory.parse(getter.responseText)
        items.size() == 2
        // in alphabetical order
        items[0].resource == 'Patient'
        items[1].resource == 'metadata'

        when: // pull only the last log event
        getter.getJson("http://localhost:8081/fproxy_war/prox/default__fhirpass/Event?_last=1")

        then:
        getter.status == 200

        when:
        items = EventStoreItemFactory.parse(getter.responseText)

        then:
        items.size() == 1
        items[0].resource == 'Patient'

        when:  // read back the Patient
        def readId = idFromUrl(cleanUrl(location))
        Patient thePatient = client.read()
        .resource(Patient)
        .withId(readId)
        .execute()

        then:
        thePatient

        when: // search for all Patients with family name Smith
        Bundle bundle = client.search()
        .forResource(Patient.class)
        .where(Patient.FAMILY.matches().values(['Smith']))
        .returnBundle(Bundle.class)
        .execute()
        println "Found ${bundle.entry.size()} Patients"

        then:
        !bundle.empty
        bundle.entry.size() > 0

        when: // search for all Patients with family name Smith and given name John
        Bundle bundle2 = client.search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().values(['Smith']))
                .and(Patient.GIVEN.matches().values(['John']))
                .returnBundle(Bundle.class)
                .execute()
        println "Found ${bundle2.entry.size()} Patients"

        then:
        !bundle2.empty
        bundle2.entry.size() > 0

        when: // search for all Patients with family name Smith and given name Jack
        Bundle bundle3 = client.search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().values(['Smith']))
                .and(Patient.GIVEN.matches().values(['Jack']))
                .returnBundle(Bundle.class)
                .execute()
        println "Found ${bundle3.entry.size()} Patients"

        then:
        bundle3.entry.size() == 0
    }

    def 'create channel with get' () {
        setup:
        deleteChannels()

        expect: // verify channel does not exist
        new HttpGet().getJson("http://localhost:8081/fproxy_war/prox/default__test").status == 404
        new HttpGet().getJson("http://localhost:8081/fproxy_war/prox/default__fhirpass").status == 404
        new HttpGet().getJson("http://localhost:8081/fproxy_war/prox/default__abc").status == 404

        when:
        ParameterBuilder pb = new ParameterBuilder()
        String request = createChannelRequest('default', 'test')
        def jsonRequest = new JsonSlurper().parseText(request)
        jsonRequest.each { String name, String value ->
            pb.add(name, value)
        }
        HttpGet getter = new HttpGet()
        getter.getJson(HttpBase.buildURI('http://localhost:8081/fproxy_war/prox', pb))
        String response = getter.responseText
        response = JsonOutput.prettyPrint(response)
        def jsonReturned = new JsonSlurper().parseText(response)

        then:
        jsonRequest == jsonReturned

        expect: // verify channel does exist
        new HttpGet().getJson("http://localhost:8081/fproxy_war/prox/default__test").status == 200

    }

    void deleteChannels() {
        new HttpDelete().run("http://localhost:8081/fproxy_war/prox/default__fhirpass")
        new HttpDelete().run("http://localhost:8081/fproxy_war/prox/default__test")
    }

    String createChannelRequest(String testSession, String id) {
        JsonOutput.prettyPrint(
                '''
         {
            "environment": "default",
            "testSession": "testSessionName",
            "channelId": "simIdName",
            "actorType": "fhir",
            "channelType": "passthrough",
            "fhirBase": "http://localhost:8080/fhir/fhir"}
        '''.replace('testSessionName', testSession).replace('simIdName', id))
    }

    String createChannel(String testSession, String id) {
        def json = createChannelRequest(testSession, id)

        HttpPost poster = new HttpPost()
        poster.postJson(new URI('http://localhost:8081/fproxy_war/prox'), json)
        assert poster.status in [200, 201]
        "${testSession}__${id}"
    }

    def withBase(String base) {
        client = ctx.newRestfulGenericClient(base)

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor()
        loggingInterceptor.setLogRequestHeaders(true)
        loggingInterceptor.setLogRequestBody(true)
        loggingInterceptor.setLogResponseHeaders(true)
        loggingInterceptor.setLogResponseBody(true)
        client.registerInterceptor(loggingInterceptor)
    }

    String createPatient(Patient patient) {
        // Invoke the server create method (and send pretty-printed JSON
        // encoding to the server
        // instead of the default which is non-pretty printed XML)
        MethodOutcome outcome = client.create()
                .resource(patient)
                .prettyPrint()
                .encodedJson()
                .execute();

        // The MethodOutcome object will contain information about the
        // response from the server, including the ID of the created
        // resource, the OperationOutcome response, etc. (assuming that
        // any of these things were provided by the server! They may not
        // always be)
        IIdType id = (IIdType) outcome.getId();
        System.out.println("Got ID: " + id.value);
        id.value
    }

    String cleanUrl(String url) {
        url = url.split('_',2)[0]
        url = url.substring(0, url.size()-1)  // remove trailing /
        url
    }

    String idFromUrl(String url) {
        String[] parts = url.split('/')
        parts[-1]
    }

}
