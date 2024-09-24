String jsonReport = new File(basedir, "target/validationReport.json").text

assert jsonReport.contains("\"totalViolations\" : 5")
assert jsonReport.contains("\"totalIgnoredViolations\" : 0")
assert jsonReport.contains("\"groupedBy\" : \"rule\"")