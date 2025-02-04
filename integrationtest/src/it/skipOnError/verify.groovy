String log = new File(basedir, "build.log").text

assert log.contains("Trailing slashes MUST NOT be used.")