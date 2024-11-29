import groovy.json.JsonSlurper

def jsonFile = new File(basedir, "target/myCustomName")
def jsonReport = new JsonSlurper().parseText(jsonFile.text)

assert jsonReport.totalViolations == 6
assert jsonReport.totalIgnoredViolations == 0
assert jsonReport.groupedBy.equals("rule")