gradle.allprojects {
	/** DEPENDENCIES */
	dependencies {

		testCompile "com.lordofthejars:nosqlunit-mongodb:$TEST_NOSQLUNIT"

		testCompile LOGGING

		testCompile tests_frameworks // TEST FRAMEWORKS

		testCompile("org.easymock:easymock:${TEST_EASYMOCK}") { exclude group: 'org.objenesis' }
		testRuntime "org.apache.logging.log4j:log4j-core:${LOG4J}"

		testCompile("org.spockframework:spock-core:$TEST_SPOCK") {
			exclude group: 'junit'
			exclude group: 'org.codehaus.groovy'
		}

		testCompile("org.spockframework:spock-spring:$TEST_SPOCK") {
			exclude group: 'junit'
			exclude group: 'org.codehaus.groovy'

		}

		testCompile("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")

		testCompile "org.springframework.security:spring-security-test:$SPRING_SECU"

		testCompile "com.jayway.jsonpath:json-path:$TEST_JAYWAY_JSONPATH"
		testCompile "com.jayway.jsonpath:json-path-assert:$TEST_JAYWAY_JSONPATH"

	}


	configurations.all {
		resolutionStrategy { //        force    'org.objenesis:objenesis:1.3'
			force "org.codehaus.groovy:groovy-all:$GROOVY_VERSION" }
	}
}
