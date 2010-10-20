import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.GrailsTestTypeResult
import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.test.support.GrailsTestTypeSupport
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory
import org.codehaus.groovy.grails.test.junit4.listener.PerTestRunListener
import org.codehaus.groovy.grails.test.io.SystemOutAndErrSwapper
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import java.lang.reflect.InvocationTargetException

eventAllTestsStart = {
	// This makes it so selenium tests are fired only when selenium is specifically
	// listed as a parameter:
	//		grails test-app :selenium
    if(targetPhasesAndTypes[TEST_PHASE_WILDCARD]?.contains('selenium'))
        binding.otherTests = [ new SeleniumTestType('selenium', 'selenium', loadConfig()) ]
}

eventTestSuiteStart = {
    type ->
    if(type=='selenium') {
        packageTests()
        bootstrap()

        // Get the Grails application instance created by the bootstrap process.
        def app = appCtx.getBean(GrailsApplication.APPLICATION_ID)
        if (app.parentContext == null) {
            app.applicationContext = appCtx
        }

        initPersistenceContext()
    }    
}

eventTestSuiteEnd = {
    type ->
    if(type=='selenium') {
        destroyPersistenceContext()
        appCtx?.close()
    }
}   

private def loadConfig() {
    def config = new ConfigSlurper(GrailsUtil.environment).parse('')

    def configFile = new File(basedir, "grails-app/conf/SeleniumConfig.groovy")
    if (configFile.isFile()) {
        def slurper = new ConfigSlurper(GrailsUtil.environment)
        config.merge slurper.parse(configFile.toURI().toURL())
    } else {
        event "StatusUpdate", ["SeleniumConfig.groovy not found. No Selenium servers defined."]
    }

    return config.selenium
}

class SeleniumTestType extends GrailsTestTypeSupport {

    def servers     = [:]
    def testClasses = []
    GrailsTestMode mode

    SeleniumTestType(String name, String sourceDirectory, config) {
        super(name, sourceDirectory)

        servers = config.servers

        mode = new GrailsTestMode(autowire:true, wrapInTransaction:false, wrapInRequestEnvironment:false)
    }

    protected List<String> getTestSuffixes() { ['Test', 'Tests'] }

    protected void eachTest(testClass, closure) {
        testClass.methods.each {
            method ->
            if(method.name.startsWith('test') && method.returnType.name=='void' && Modifier.isPublic(method.modifiers) && method.parameterTypes.size()==0)
                closure(method)
        }
    }

    protected int doPrepare() {
        eachSourceFile {
            testTargetPattern, sourceFile ->
            def testClass = sourceFileToClass(sourceFile)
            if (!Modifier.isAbstract(testClass.modifiers)) {
                testClasses << testClass
            }
        }

        def count = 0
        testClasses.each { testClass -> eachTest(testClass) { count ++ } }

        return count * servers.size()
    }

    protected GrailsTestTypeResult doRun(GrailsTestEventPublisher eventPublisher) {
        def failCount = 0
        def passCount = 0

        def reportsFactory = JUnitReportsFactory.createFromBuildBinding(buildBinding)

        for (testClass in testClasses) {

            PerTestRunListener listener = new PerTestRunListener(testClass.name, eventPublisher, reportsFactory.createReports(testClass.name), new SystemOutAndErrSwapper())

            listener.start()

            eachTest(testClass) {
                test ->

                def threads = servers.collect {
                    server ->

                    def description = Description.createTestDescription(testClass, "$test.name ($server.name)")

                    Thread.start {
                        listener.testStarted description

                        def instance = testClass.newInstance()

                        mode.createInterceptor(instance, applicationContext, new String[0]).wrap {
                            try {
                                instance.setUp(server.name, server.host, server.port, server.browser, server.url)
                                instance.setUp()
                                test.invoke(instance)
                                passCount++
                            }
                            catch (Throwable e) {
                                failCount++
                                if(e instanceof InvocationTargetException)
                                    e = e.targetException
                                listener.testFailure new Failure(description, e)
                            }
                            finally {
                                instance.tearDown()
                            }
                        }   

                        listener.testFinished description
                    }
                }

                threads*.join()
            }

            listener.finish()
        }

        return ([getFailCount:{failCount}, getPassCount:{passCount}] as GrailsTestTypeResult)
    }
}