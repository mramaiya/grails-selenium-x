class SeleniumXGrailsPlugin {
    def version = "0.1"
    def grailsVersion = "1.3.4 > *"
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Milan Ramaiya"
    def authorEmail = "milan.ramaiya@gmail.com"
    def title = "Selenium-x"
    def description = 'Selenium RC driver with support for parallel test execution on multiple servers and full database integration.'

    def documentation = "http://grails.org/plugin/selenium-x"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
