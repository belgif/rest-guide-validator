String log = new File(basedir, "build.log").text

assert log.contains("contains a circular reference to itself")
