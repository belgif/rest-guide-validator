String log = new File(basedir, "build.log").text

assert log.contains("[INFO] Start creation of KieBase: defaultKieBase")
assert log.contains("It does not make sense to write a mediaType as lowerCamelCase")
assert log.contains("4 ignored violations")
assert log.contains("not of type Object with rules/reasons as keys and values")
assert log.contains("not of type String")
assert log.contains("[IGNORED]      [cod-design]")