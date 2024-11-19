A windows installer can be built (locally for now) by invoking the following command in the cli/target folder after a maven build.

To inspect which modules are actually needed for the jar to run:
```bash
jdeps --print-module-deps --ignore-missing-deps .\belgif-rest-guide-validator-cli-latest.jar
```

To build a custom JRE with only the needed dependencies use
```bash
jlink --add-modules java.base,java.naming,java.xml,java.desktop --output custom-jre --strip-debug --no-header-files --no-man-pages
```

To build the installer with a custom JRE (for windows) use:
```bash
jpackage --input . --name belgif-validate-openapi --main-jar belgif-rest-guide-validator-cli-latest.jar --type msi --app-version 2.2.0 --description "Validate OpenApi to Belgif guidelines" --vendor "Belgif" --icon ../package/belgif.ico --win-console --resource-dir "../package/windows" --runtime-image custom-jre --install-dir belgif-rest-guide-validator --add-launcher launch-belgif-rest-guide-validator="../package/clickable-launcher.properties"
```