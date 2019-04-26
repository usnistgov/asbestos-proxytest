package gov.nist.asbestos.proxytest

import spock.lang.Specification

class UrlSpec extends Specification {


    def 'foo' () {
        when:
        HttpURLConnection post = new URL('http://localhost:8080/fproxy_war').openConnection()
        def message = '{"message":"this is a message"}'
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(message.getBytes("UTF-8"))
        def postRC = post.getResponseCode();

        then:
        postRC == 200
    }


    def 'bar' () {
        when:
        HttpURLConnection post = new URL('http://localhost:8888/sim/default__rr/reg/rb').openConnection()
        def message = '{"message":"this is a message"}'
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(message.getBytes("UTF-8"))
        def postRC = post.getResponseCode();

        then:
        postRC == 200
    }
}
