package com.seleniumx
                                                  
import com.thoughtworks.selenium.*

import java.util.regex.Pattern

abstract class SeleniumTestCase extends GroovyTestCase {

    def selenium
    def defaultTimeout = 30000
    def server

    public void setUp(String name, String server, int port=4444, String browser, String url) {
        selenium = new com.thoughtworks.selenium.GroovySelenium(new com.thoughtworks.selenium.DefaultSelenium(server, port, browser, url))
        start()
        windowMaximize()

        this.server = name
    }

    void tearDown() {
        close()
        stop()
    }

    def methodMissing(String name, args) {
        selenium.invokeMethod(name, args)
    }

    def assertMatches(Pattern pattern, String value) {
        assertTrue pattern.matcher(value).matches()
    }

    void type(Map data) {
        data.each {
            field, text ->
            type(field, text)
        }
    }

    void waitFor(String message='timeout', int timeout=defaultTimeout, Closure condition) {
        assert timeout > 0
        def timeoutTime = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < timeoutTime) {
            try {
                if (condition.call())
                    return
                sleep(500)
            } catch(com.thoughtworks.selenium.SeleniumException e) {
                if(! e.message =~ /Couldn't access document body./)
                    throw e
            }
        }

        fail(message)
    }
}