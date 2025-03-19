This project is build using Maven and GitHub actions.
The release process of this project is documented [here](https://github.com/belgif/openapi-common/blob/master/BUILDING.md).

To build the windows installer locally, invoke the following command in the cli/target folder after a maven build.

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
jpackage --input . --name belgif-validator-rest --main-jar belgif-rest-guide-validator-cli-latest.jar --type msi --app-version 2.2.0 --description "Validate OpenApi to Belgif guidelines" --vendor "Belgif" --icon ../package/belgif.ico --win-console --resource-dir "../package/windows" --runtime-image custom-jre --install-dir belgif-rest-guide-validator --file-associations ..\package\file-associations\FAyaml.properties --file-associations ..\package\file-associations\FAjson.properties --add-launcher belgif-validate-openapi="../package/cli-launcher.properties" --win-per-user-install
```


**Native Quarkus compilation**

Prereq:
* download latest JDK21 version of Mandrel - https://github.com/graalvm/mandrel/releases
* unpack it
* add GRAALVM_HOME=<directory>\mandrel-java21-<xxx>-Final env variable
* Add C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build to your PATH
* download and install Visual Studio community edition with these install options: https://www.graalvm.org/latest/getting-started/windows/#install-visual-studio-build-tools-and-windows-sdk
