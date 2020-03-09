import fr.cs.eo.utils.TuleapSpockTestBase
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

class uploadFileSpockTestSpec extends TuleapSpockTestBase {

    def "test with file"() {

        given:
        def options = [
            tuleapServer: mockServerURL.toString(),
            accessKey: "123456789",
            releaseId: 242,
            fileName: "payload",
            filePath: "test/resources/payload"
        ]

        and:
        binding.getVariable('currentBuild').result = 'SUCCESS'

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_release/242/files"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody("{}"));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_files"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody('{"upload_href":"/api/frs_files/content/342"}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_files/content/342")
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", "0")
                        .withHeader("Content-Type", "application/offset+octet-stream"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withHeader("Tus-Resumable", "1.0.0")
                           .withHeader("Upload-Offset", "10000"));

        when:
        def script = loadScript('vars/uploadTuleapFile.groovy')
        script.call(options)

        then:
        printCallStack()
        assertJobStatus('SUCCESS')
    }

    def "test with empty package"() {

        given:
        def options = [
            tuleapServer: mockServerURL.toString(),
            accessKey: "123456789",
            packageId: 142,
            releaseName: "release",
            fileName: "payload",
            filePath: "test/resources/payload"
        ]

        and:
        binding.getVariable('currentBuild').result = 'SUCCESS'

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_packages/142/frs_release"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody('{"collection":[],"total_size":0}'));


        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_release/242/files"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody("{}"));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_files"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody('{"upload_href":"/api/frs_files/content/342"}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_files/content/342")
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", "0")
                        .withHeader("Content-Type", "application/offset+octet-stream"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withHeader("Tus-Resumable", "1.0.0")
                           .withHeader("Upload-Offset", "10000"));


        when:
        def script = loadScript('vars/uploadTuleapFile.groovy')
        script.call(options)

        then:
        printCallStack()
        assertJobStatus('SUCCESS')
    }

    def "test with named parameters"() {

        given:
        def options = [
            tuleapServer: mockServerURL.toString(),
            accessKey: "123456789",
            projectId: 42,
            packageName: "package",
            releaseName: "release",
            fileName: "payload",
            filePath: "test/resources/payload"
        ]

        and:
        binding.getVariable('currentBuild').result = 'SUCCESS'

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/projects/42/frs_packages"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody('[{"label":"package","id":"142"}]'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_packages/142/frs_release"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody('{"collection":[{"name":"release","id":"242"}]}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_release/242/files"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody("{}"));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_files"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withBody('{"upload_href":"/api/frs_files/content/342"}'));

        and:
        mockServer.when(new HttpRequest()
                        .withPath("/api/frs_files/content/342")
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", "0")
                        .withHeader("Content-Type", "application/offset+octet-stream"))
                  .respond(new HttpResponse()
                           .withStatusCode(201)
                           .withHeader("Tus-Resumable", "1.0.0")
                           .withHeader("Upload-Offset", "10000"));


        when:
        def script = loadScript('vars/uploadTuleapFile.groovy')
        script.call(options)

        then:
        printCallStack()
        assertJobStatus('SUCCESS')
    }
}
