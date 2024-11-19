import groovy.xml.XmlSlurper

String log = new File(basedir, "build.log").text

def xmlFile = new File(basedir, "target/TEST-[prb-defaul].xml")
def junitReport = new XmlSlurper().parseText(xmlFile.text)

assert log.contains("[INFO] Start creation of KieBase: defaultKieBase")
assert log.contains("[MANDATORY]")
assert log.contains("At least 1 error in validation !")
assert junitReport.@time.toFloat() instanceof Float
assert junitReport.testcase.@time.toFloat() instanceof Float