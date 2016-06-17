gradle.allprojects {
	//TEST FRAMEWORKS DEPENDENCIES

	ext.JUNIT= "junit:junit:${TEST_JUNIT}"

	ext.LOGGING = "ch.qos.logback:logback-classic:$LOG_LOGBACK"

	ext.tests_frameworks = [
		JUNIT,
		"org.mockito:mockito-core:${TEST_MOCKITO}",
		"org.powermock:powermock-mockito-release-full:${TEST_POWERMOCK}",
		"org.powermock:powermock-module-junit4-rule:${TEST_POWERMOCK}",
		"org.powermock:powermock-classloading-xstream:${TEST_POWERMOCK}",
		"cglib:cglib-nodep:${TEST_CGLIB}",
		"org.hamcrest:hamcrest-core:${TEST_HAMCREST}",
		"org.objenesis:objenesis:${TEST_OBJENESIS}",
		/*	"com.tocea.frameworks:bench4j-core:${TEST_BENCH4J}",
		 "com.tocea.frameworks:bench4j-htmlreport:${TEST_BENCH4J}" */
	]

}