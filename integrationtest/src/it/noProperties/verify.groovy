String log = new File(basedir, "build.log").text

assert log.contains("api-validator need at least one file ! ")
