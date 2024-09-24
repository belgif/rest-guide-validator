import groovy.json.JsonSlurper

def jsonFile = new File(basedir, "target/validationReport.json")
def jsonReport = new JsonSlurper().parseText(jsonFile.text)

assert jsonReport.totalViolations == 5
assert jsonReport.totalIgnoredViolations == 0
assert jsonReport.groupedBy.equals("rule")