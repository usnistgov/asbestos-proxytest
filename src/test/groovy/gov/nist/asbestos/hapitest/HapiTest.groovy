package gov.nist.asbestos.hapitest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.primitive.IdDt
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.instance.model.Bundle
import org.hl7.fhir.dstu3.model.Patient
import spock.lang.Shared
import spock.lang.Specification

class HapiTest extends Specification {
    @Shared FhirContext ctx = FhirContext.forDstu3()
    @Shared String serverBase = "http://localhost:8080/fhir"
    @Shared IGenericClient client = ctx.newRestfulGenericClient(serverBase)

    def 'fhir create patient' () {
        when:
        Patient patient = new Patient();
// ..populate the patient object..
        patient.addIdentifier().setSystem("urn:system").setValue("12345");
        patient.addName().setFamily("Smith").addGiven("John");

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

        then:
        id

    }

    def 'fhir test 2' () {
        when:
        Bundle results = client
                .search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().value("duck"))
                .returnBundle(Bundle)
                .execute();

        System.out.println("Found " + results.getEntry().size() + " patients named 'duck'");

        then:
        true
    }
}
