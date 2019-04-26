package gov.nist.asbestos.proxytest

import spock.lang.Specification
import groovyx.net.http.HTTPBuilder

class UrlSpec extends Specification {

    def uri = ''
    def http = new HTTPBuilder(uri)
}
