package gov.nist.asbestos.hapitest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.primitive.IdDt
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import gov.nist.asbestos.adapter.HttpPost
import org.hl7.fhir.dstu3.model.Patient
import spock.lang.Shared
import spock.lang.Specification

class HapiTest extends Specification {
    @Shared FhirContext ctx = FhirContext.forDstu3()
    IGenericClient client

    def 'fhir create patient' () {
        setup:
        withBase("http://localhost:8080/fhir")

        when:
        Patient patient = new Patient();
        // ..populate the patient object..
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        def id = createPatient(patient)

        then:
        id

    }


    def 'fhir create patient through proxy' () {
        setup:
        withBase("http://localhost:8080/fproxy_war/prox/${createChannel('default', 'patient1')}")

        when:
        Patient patient = new Patient();
        // ..populate the patient object..
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");
        def id = createPatient(patient)

        then:
        id

    }

    String createChannel(String testSession, String id) {
        def json = '''
{
  "environment": "default",
  "testSession": "testSessionName",
  "simId": "simIdName",
  "actorType": "balloon"}
'''.replace('testSessionName', testSession).replace('simIdName', id)

        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox', json)
        assert rc in [200, 201]
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
        IdDt id = (IdDt) outcome.getId();
        System.out.println("Got ID: " + id.getValue());
        id.value
    }

}
