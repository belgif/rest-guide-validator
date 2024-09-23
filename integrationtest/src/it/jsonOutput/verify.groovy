String jsonReport = new File(basedir, "target/validationReport.json").text

assert jsonReport.contains("\"violationCount\" : 5")
assert jsonReport.contains("\"ignoredViolationCount\" : 0")
assert jsonReport.contains("\"groupedBy\" : \"RULE\"")