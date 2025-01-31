String log = new File(basedir, "build.log").text
String junit = new File(basedir, "target/surefire-reports/TEST-[cod-design].xml").text

assert ! log.contains("[ERROR]")
assert junit.contains("failures=\"1\" name=\"[cod-design]\" skipped=\"1\" tests=\"2\"")
