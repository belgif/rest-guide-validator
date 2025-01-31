String log = new File(basedir, "build.log").text

assert log.contains("not supported")