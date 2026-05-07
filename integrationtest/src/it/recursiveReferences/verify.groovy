String log = new File(basedir, "build.log").text

assert log.contains("OpenApi validation summary: 1 violations")
assert log.contains("[RECOMMENDED]")
