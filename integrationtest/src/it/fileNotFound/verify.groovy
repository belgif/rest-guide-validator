String log = new File(basedir, "build.log").text

assert log.contains("File not found")
