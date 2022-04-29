String log = new File(basedir, "build.log").text

assert log.contains("[INFO] Start creation of KieBase: defaultKieBase")
assert log.contains("[MANDATORY]\tPathTrailingSlash\tTrailing slashes MUST NOT be used. \t/userInfo/")
