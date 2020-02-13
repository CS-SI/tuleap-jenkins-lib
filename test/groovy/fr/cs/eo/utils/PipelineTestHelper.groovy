package fr.cs.eo.utils

import com.lesfurets.jenkins.unit.BasePipelineTest
import static com.lesfurets.jenkins.unit.MethodSignature.method
import static org.assertj.core.api.Assertions.assertThat

class PipelineTestHelper extends BasePipelineTest {

    /**
     * Override the setup for our purposes
     */
    @Override
    void setUp() {

        // Scripts (Jenkinsfiles etc) loaded from root of project directory and have no extension by default
        helper.scriptRoots = ['']
        helper.scriptExtension = ''

        // Add support to the helper to unregister a method
        helper.metaClass.unRegisterAllowedMethod = { String name, List<Class> args ->
            allowedMethodCallbacks.remove(method(name, args.toArray(new Class[args.size()])))
        }

        // Setup the parent stuff
        super.setUp()

        // Declaring all my stuff
        registerDeclarativeMethods()
        registerScriptedMethods()
        setJobVariables()
    }

    /**
     * Declarative pipeline methods not in the base
     *
     * See here:
     * https://www.cloudbees.com/sites/default/files/declarative-pipeline-refcard.pdf
     */
    void registerDeclarativeMethods() {
        helper.registerAllowedMethod('retry', [Integer.class, Closure.class], { Integer nb, Closure c -> c() })
        helper.registerAllowedMethod("error", [String.class], { String m -> updateBuildStatus('FAILURE'); throw new Exception(m) })
    }

    /**
     * Scripted pipeline methods not in the base
     */
    void registerScriptedMethods() {

    }

    /**
     * Variables that Jenkins expects
     */
    void setJobVariables() {

    }

    /**
     * Prettier print of call stack to whatever taste
     */
    @Override
    void printCallStack() {
        println '>>>>>> pipeline call stack -------------------------------------------------'
        super.printCallStack()
        println ''
    }

    /**
     * Helper for adding a params value in tests
     */
    void addParam(String name, Object val, Boolean overWrite = false) {
        Map params = binding.getVariable('params') as Map
        if (params == null) {
            params = [:]
            binding.setVariable('params', params)
        }
        if ( (val != null) && (params[name] == null || overWrite)) {
            params[name] = val
        }
    }

    /**
     * Helper for adding a environment value in tests
     */
    void addEnvVar(String name, String val) {
        if (!binding.hasVariable('env')) {
            binding.setVariable('env', new Expando(getProperty: { p -> this[p] }, setProperty: { p, v -> this[p] = v }))
        }
        def env = binding.getVariable('env') as Expando
        env[name] = val
    }

    void assertJobStatus(String status) {
        assertThat(binding.getVariable('currentBuild').result).isEqualTo(status)
    }

}
