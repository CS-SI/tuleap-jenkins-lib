import fr.cs.eo.utils.TuleapSpockTestBase
import spock.lang.Unroll
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

class sendStatusSpockTestSpec extends TuleapSpockTestBase {

    @Unroll
    def "test shared library code - #RESULT"() {

        given:
        def options = [
            gitToken: "12345678890",
            tuleapServer: mockServerURL,
            targetRepo: 'project-name/repo-name.git'
        ]

        and:
        binding.getVariable('currentBuild').result = RESULT

        and:
        binding.getVariable('currentBuild').resultIsBetterOrEqualTo = { s ->
            // s is expected to be SUCCESS
            if (s == RESULT)
                return true;
            else
                return false;
        }

        and:
        binding.setVariable('env', [ GIT_COMMIT: "0000"])

        and:
        mockServer.when(new HttpRequest()
                  .withPath("/git/project-name%2Frepo-name.git/statuses/0000")
                  .withBody("{\"state\":\""+STATE+"\",\"token\":\"12345678890\"}"))
                  .respond(new HttpResponse()
                          .withStatusCode(201));

        when:
        def script = loadScript('vars/sendTuleapStatus.groovy')
        script.call(options)

        then:
        printCallStack()
        assertJobStatus(RESULT)

        where:
        RESULT     | STATE     | NONE
        'SUCCESS'  | "success" | _
        'FAILURE'  | "failure" | _
        'ABORTED'  | "failure" | _
        'UNSTABLE' | "failure" | _
    }
}
