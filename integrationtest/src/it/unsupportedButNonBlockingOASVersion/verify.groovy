String log = new File(basedir, "build.log").text

assert log.contains("OpenApi validation summary")
assert log.contains("version <<3.1.0>> is not supported")
