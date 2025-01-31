String log = new File(basedir, "build.log").text

assert log.contains("Input files with OAS2 / Swagger specification are not supported.")