String log = new File(basedir, "build.log").text

assert log.contains("[Internal error] File: openapi.yaml appears to be empty!")