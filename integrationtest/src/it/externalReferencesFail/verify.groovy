String log = new File(basedir, "build.log").text

assert log.contains("[INFO] Start creation of KieBase: defaultKieBase")
assert log.contains("[MANDATORY]")
assert log.contains("At least 1 error in validation !")
assert log.contains("[propertyName: Image]")
assert log.contains("/components/schemas/Logo")
