package gov.nist.asbestos.proxytest

import spock.lang.Specification

class UrlSpec extends Specification {


    def 'create channel' () {
        when:
        def rc = post('http://localhost:8080/fproxy_war/prox',
                '''
{
  "environment": "default",
  "testSession": "default",
  "simId": "1",
  "actorType": "balloon"
}
''')

        then:
        rc == 201 || rc == 200
    }

    def 'delete channel' () {
        when:
        def rc = http('DELETE', 'http://localhost:8080/fproxy_war/prox/default__1', null)

        then:
        rc == 200
    }

    def 'recreate channel' () {
        when:
        def rc = post('http://localhost:8080/fproxy_war/prox',
                '''
{
  "environment": "default",
  "testSession": "default",
  "simId": "1",
  "actorType": "balloon"
}
''')

        then:
        rc == 201
    }

    def 're-recreate channel' () {
        when:
        def rc = post('http://localhost:8080/fproxy_war/prox',
                '''
{
  "environment": "default",
  "testSession": "default",
  "simId": "1",
  "actorType": "balloon"
}
''')

        then:
        rc == 200
    }



    def 'post to proxy - no actor' () {
        when:
        def rc = post('http://localhost:8080/fproxy_war/prox/default__1', '{"message":"this is a message"}')

        then:
        rc == 403  //
    }

    def 'post to actor balloon - no transaction' () {
        when:
        def rc = post('http://localhost:8080/fproxy_war/prox/default__1/balloon', '{"message":"this is a message"}')

        then:
        rc == 403  //
    }

    def 'post to actor balloon' () {
        when:
        def rc = post('http://localhost:8080/fproxy_war/prox/default__1/balloon/pop', '{"message":"this is a message"}')

        then:
        rc == 200  //
    }


    def post(String url, String json) {
        http('POST', url, json)
    }

    def http(String op, String url, String json) {
        HttpURLConnection http = new URL(url).openConnection()
        http.setRequestMethod(op)
        http.setDoOutput(true)
        http.setRequestProperty("Content-Type", "application/json")
        if (json)
            http.getOutputStream().write(json.getBytes("UTF-8"))
        http.getResponseCode()
    }

}
