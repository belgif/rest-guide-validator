String log = new File(basedir, "build.log").text

assert log.contains("rest-guide-validator need at least one file ! ")
