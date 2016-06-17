allprojects {

	/** TESTS */
	jacoco { toolVersion = '0.7.1.201405082137' }
}

subprojects {

	jacocoTestReport {
		reports {
			html.enabled = true
			xml.enabled = true
			csv.enabled = false
		}
	}

	task smokeTest(type: Test) { useJUnit { includeCategories 'fr.echoes.labs.ksf.foundation.tests.SmokeTests' } }

	task seleniumTest(type: Test) { useJUnit { includeCategories 'fr.echoes.labs.ksf.foundation.tests.SeleniumTests' } }
}


task jacocoRootReport(type: org.gradle.testing.jacoco.tasks.JacocoReport) {
	dependsOn = subprojects.test
	sourceDirectories = files(subprojects.sourceSets.main.allSource.srcDirs)
	classDirectories = files(subprojects.sourceSets.main.output)
	executionData = files(subprojects.jacocoTestReport.executionData)
	reports {
		html.enabled = true
		xml.enabled = true
		csv.enabled = false
		xml.destination = "${buildDir}/reports/jacoco/test/jacocoTestReport.xml"
	}
	onlyIf = { true }
	doFirst {
		executionData = files(executionData.findAll { it.exists() })
	}

	tasks.withType(Test) {
		ignoreFailures=project.hasProperty("ignoreTestFailures")
	}
}
