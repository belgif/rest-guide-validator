String log = new File(basedir, "build.log").text

assert log.contains("Swagger are not supported")