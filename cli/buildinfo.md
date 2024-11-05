A windows installer can be built (locally for now) by invoking the following command in the cli/target folder after a maven build.
```bash
jpackage --input . --name belgif-validate-openapi --main-jar belgif-rest-guide-validator-cli-latest.jar --type msi --app-version 2.2.0 --description "The belgif-rest-guide-validator is used to validate if an OpenAPI document conforms to the guidelines in the Belgif REST guide." --vendor "Belgif" --icon ../package/belgif.ico --win-console --resource-dir "../package/windows"
```