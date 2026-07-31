./qa-lib/ant/bin/ant checkstyle jacoco test
java -cp "test-bin:bin:lib/*:qa-lib/*" org.pitest.mutationtest.commandline.MutationCoverageReport --reportDir reports/pitest --targetClasses "cds.aladin.CatalogDownloader" --targetTests "*Test" --sourceDirs src
