String log = new File(basedir, "build.log").text

assert log.contains("rest-guide-validator needs at least one file ! ")
