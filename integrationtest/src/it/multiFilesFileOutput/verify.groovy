import groovy.json.JsonSlurper

def jsonFile = new File(basedir, "target/validationReport.json")
def jsonReport = new JsonSlurper().parseText(jsonFile.text)

assert jsonReport.violations.containsKey("petstore.json")
assert jsonReport.violations.containsKey("swagger.yaml")