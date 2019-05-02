package gov.nist.asbestos.proxytest


import gov.nist.asbestos.adapter.HttpPost
import spock.lang.Specification

class CreateChannelSpec extends Specification {


    def 'create channel' () {
        when:
        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox',
                '''
{
  "environment": "default",
  "testSession": "default",
  "simId": "1",
  "actorType": "balloon",
  "a": "x",
  "b": "y"
}
''')

        then:
        rc == 201 || rc == 200
    }

    def 'delete channel' () {
        when:
        def rc = HttpPost.http('DELETE', 'http://localhost:8080/fproxy_war/prox/default__1', null)

        then:
        rc == 200
    }

    def 'recreate channel' () {
        when:
        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox',
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
        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox',
                '''
{
  "environment": "default",
  "testSession": "default",
  "simId": "1",
  "actorType": "balloon"}
''')

        then:
        rc == 200
    }



    def 'post to proxy - no actor' () {
        when:
        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox/default__1', '{"message":"this is a message"}')

        then:
        rc == 403  //
    }

    def 'post to actor balloon - no transaction' () {
        when:
        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox/default__1/balloon', '{"message":"this is a message"}')

        then:
        rc == 403  //
    }

    def 'post to actor balloon' () {
        when:
        def rc = HttpPost.postJson('http://localhost:8080/fproxy_war/prox/default__1/balloon/pop', '{"message":"this is a message"}')

        then:
        rc == 200  //
    }

}
