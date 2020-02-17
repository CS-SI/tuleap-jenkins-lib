import fr.cs.eo.utils.TuleapSpockTestBase
import spock.lang.Unroll
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

class labelPRSpockTest extends TuleapSpockTestBase {

    @Unroll
    def "test add labels"() {

        given:
        def options = [
            tuleapServer: this.mockServerURL,
            accessKey: "1234567890",
            targetRepo: "repo-name",
            targetProject: "project-name",
            addLabels: ["good"]
        ]

        and:
        binding.setVariable('env', [GIT_COMMIT: '0000'])

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/projects')
                        .withMethod('GET')
                        .withQueryStringParameter('query', '{"shortname":"' + options["targetProject"] + '"}')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withBody('[{"id":42}]'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/projects/42/git')
                        .withMethod('GET')
                        .withQueryStringParameter('offset', '0')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withHeader('x-pagination-size', '1')
                        .withHeader('x-pagination-limit', '50')
                        .withBody('{"repositories":[{"id":21, "name":"' + options["targetRepo"] + '"}]}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/git/21/pull_requests')
                        .withMethod('GET')
                        .withQueryStringParameter('query', '{"status":"open"}')
                        .withQueryStringParameter('offset', '0')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withHeader('x-pagination-size', '1')
                        .withHeader('x-pagination-limit', '50')
                        .withBody('{"collection":[{"id":6}]}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/pull_requests/6')
                        .withMethod('GET')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withBody('{"id":6, "reference_src":"0000"}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/pull_requests/6/labels')
                        .withMethod('POST')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json')
                        .withHeader('X-HTTP-Method-Override', 'PATCH')
                        .withBody('{"add":[{"label":"good"}],"remove":null}'))
                  .respond(new HttpResponse()
                        .withStatusCode(200));

        when:
        def script = loadScript('vars/labelPR.groovy')
        script.call(options)

        then:
        printCallStack()
        assertJobStatus('SUCCESS')
    }

    @Unroll
    def "test remove labels"() {

        given:
        def options = [
            tuleapServer: this.mockServerURL,
            accessKey: "1234567890",
            targetRepo: "repo-name",
            targetProject: "project-name",
            rmLabels: ["good"]
        ]

        and:
        binding.setVariable('env', [GIT_COMMIT: '0000'])

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/projects')
                        .withMethod('GET')
                        .withQueryStringParameter('query', '{"shortname":"' + options["targetProject"] + '"}')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withBody('[{"id":42}]'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/projects/42/git')
                        .withMethod('GET')
                        .withQueryStringParameter('offset', '0')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withHeader('x-pagination-size', '1')
                        .withHeader('x-pagination-limit', '50')
                        .withBody('{"repositories":[{"id":21, "name":"' + options["targetRepo"] + '"}]}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/git/21/pull_requests')
                        .withMethod('GET')
                        .withQueryStringParameter('query', '{"status":"open"}')
                        .withQueryStringParameter('offset', '0')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withHeader('x-pagination-size', '1')
                        .withHeader('x-pagination-limit', '50')
                        .withBody('{"collection":[{"id":6}]}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/pull_requests/6')
                        .withMethod('GET')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withBody('{"id":6, "reference_src":"0000"}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/projects/42/labels')
                        .withMethod('GET')
                        .withQueryStringParameter('offset', '0')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json'))
                  .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withHeader('x-pagination-size', '1')
                        .withHeader('x-pagination-limit', '50')
                        .withBody('{"labels":[{"id":1,"label":"good"}]}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath('/pull_requests/6/labels')
                        .withMethod('POST')
                        .withHeader('X-Auth-AccessKey', '1234567890')
                        .withHeader('Accept', 'application/json')
                        .withHeader('X-HTTP-Method-Override', 'PATCH')
                        .withBody('{"add":null,"remove":[{"id":1}]}'))
                  .respond(new HttpResponse()
                        .withStatusCode(200));

        when:
        def script = loadScript('vars/labelPR.groovy')
        script.call(options)

        then:
        printCallStack()
        assertJobStatus('SUCCESS')
    }

    @Unroll
    def "test missing GIT_COMMIT environement"() {

        given:
        def options = [
            tuleapServer: this.mockServerURL,
            accessKey: "1234567890",
            targetRepo: "repo-name",
            targetProject: "project-name",
            rmLabels: ["good"]
        ]

        and:
        binding.setVariable('env', [])

        when:
        def script = loadScript('vars/labelPR.groovy')
        script.call(options)

        then:
        thrown(Exception)
        printCallStack()
        assertJobStatus('FAILURE')
    }
}
