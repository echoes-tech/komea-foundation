gradle.allprojects {


	task unitTest(type : Test) { exclude "**/**IT" }


	test {

		// exclude Selenium Tests
		useJUnit {
			excludeCategories 'fr.echoes.labs.ksf.foundation.tests.SeleniumTests'
			excludeCategories 'fr.echoes.labs.ksf.foundation.tests.SmokeTests'
		}

		if (project.hasProperty("logTest")) {
			// listen to events in the test execution lifecycle
			beforeTest { descriptor ->
				logger.lifecycle("Running test: " + descriptor)

			}
			// listen to standard out and standard error of the test JVM(s)
			onOutput { descriptor, event ->
				logger.lifecycle("Test: " + descriptor + " : " + event.message )
			}
			// show standard out and standard error of the test JVM(s) on the console
			testLogging.showStandardStreams = true

		}

		// set heap size for the test JVM(s)
		minHeapSize = "256m"
		maxHeapSize = "1024m"

		// set JVM arguments for the test JVM(s)
		jvmArgs '-XX:MaxPermSize=256m'

	}


	configurations.all {
		resolutionStrategy { //        force    'org.objenesis:objenesis:1.3'
			force JUNIT }
	}


}