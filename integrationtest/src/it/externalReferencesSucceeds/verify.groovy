String log = new File(basedir, "build.log").text

assert log.contains("OpenApi validation summary")
assert ! log.contains("[ERROR]")
