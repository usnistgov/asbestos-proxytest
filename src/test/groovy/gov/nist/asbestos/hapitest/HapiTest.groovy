package gov.nist.asbestos.hapitest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import gov.nist.asbestos.simapi.http.HttpDelete
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.http.HttpPost
import gov.nist.asbestos.simapi.sim.basic.EventStoreItem
import gov.nist.asbestos.simapi.sim.basic.EventStoreItemFactory
import org.hl7.fhir.dstu3.model.Patient
import org.hl7.fhir.instance.model.api.IIdType
import spock.lang.Shared
import spock.lang.Specification

class HapiTest extends Specification {
    @Shared FhirContext ctx = FhirContext.forDstu3()
    IGenericClient client

    def 'fhir create patient through proxy' () {
        setup:  // create channel
        deleteChannel()
        withBase("http://localhost:8081/fproxy_war/prox/${createChannel('default', 'fhirpass')}/Channel")

        when: // submit patient resource
        Patient patient = new Patient();
        // ..populate the patient object..
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        def id = createPatient(patient)

        then:  // if successful then id assigned
        id

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
    }

    void deleteChannel() {
        new HttpDelete().run("http://localhost:8081/fproxy_war/prox/default__fhirpass")
    }

    String createChannel(String testSession, String id) {
        def json = '''
{
  "environment": "default",
  "testSession": "testSessionName",
  "channelId": "simIdName",
  "actorType": "fhir",
  "channelType": "passthrough",
  "fhirBase": "http://localhost:8080/fhir/fhir/"}
'''.replace('testSessionName', testSession).replace('simIdName', id)

        HttpPost poster = new HttpPost()
        poster.postJson('http://localhost:8081/fproxy_war/prox', json)
        assert poster.status in [200, 201]
        "${testSession}__${id}"
    }

    def withBase(String base) {
        client = ctx.newRestfulGenericClient(base)
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

}
