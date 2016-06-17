gradle.allprojects {
	/** FINDBUGS -------------------------------------------------------------- */

	// Marker Task to enable findbugs.
	task findbugs(
			group: "Verification",
			description: """Marker task to enabled findbugs. Findbugs is by default
                        disabled. E.g. ( ./gradlew findbugs build )"""
			)

	gradle.taskGraph.whenReady { taskGraph ->
		tasks.findbugsMain.onlyIf { taskGraph.hasTask((tasks.findbugs)) }
		tasks.findbugsTest.onlyIf { taskGraph.hasTask((tasks.findbugs)) }
	}

	findbugs {
		ignoreFailures = true
		effort = "max"

	}

	tasks. withType(FindBugs) {
		reports { xml.enabled=true }
	}
}