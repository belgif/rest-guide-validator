String log = new File(basedir, "build.log").text

assert log.contains("Input file is not an OpenApi file")