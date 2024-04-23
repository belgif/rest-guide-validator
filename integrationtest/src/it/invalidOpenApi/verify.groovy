String log = new File(basedir, "build.log").text

assert log.contains("[INFO] Start creation of KieBase: defaultKieBase")
assert log.contains("Input file is not an OpenApi or Swagger file")
