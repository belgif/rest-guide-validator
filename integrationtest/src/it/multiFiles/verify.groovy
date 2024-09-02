String log = new File(basedir, "build.log").text

assert log.contains("[INFO] Start creation of KieBase: defaultKieBase")
assert log.contains("[Operation: GET /myOnlyPath]")
assert log.contains("The name of a tag SHOULD start with a capital letter.")
assert log.contains("Tag: <<pet>>")