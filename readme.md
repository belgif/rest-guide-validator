# belgif-rest-guide-validator plugin

The belgif-rest-guide-validator Plugin is used to validate a Swagger API to conforms the Belgif standards (https://www.belgif.be/specification/rest/api-guide/).

# Prerequisites
This Maven plugin requires JDK 17 or higher to be used in the Maven runtime. 
Note that the JDK version used to compile the source code of the project can differ. 
IDEs may need manual configuration to set the appropriate Maven runtime JDK version.
The minimum maven version is 3.8.5
## Goal Overview
The goal api-validator has as default phase the LifecyclePhase.PREPARE_PACKAGE.

## Usage
The plugin is commonly used to verify the openApi files for a project.

### Basic example
The following example demonstrates a basic plugin configuration for validating one design first api file.
```xml
<plugins>
    <plugin>
        <groupId>io.github.belgif.rest.guide.validator</groupId>
        <artifactId>belgif-rest-guide-validator-maven-plugin</artifactId>
        <version>2.0.0</version> <!-- update this to the latest version -->
        <executions>
            <execution>
                <goals>
                    <goal>api-validator</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <files>
                <file>src/main/resources/openapi.yaml</file>
            </files>
        </configuration>
    </plugin>
</plugins>
```
You can now build you package as usual with:
```bash
mvn package  
```
### Exclusions in OpenApi file

The `x-ignore-rules` property can be added inside a yaml object in the OpenAPI document to ignore this object for one or more rules.


_Example_

To ignore following `cod-design` validation error, add the `x-ignore-rules` property to the exact object mentioned in the error message (`/components/schemas/BelgianRegionCode`).

Error message:
```
file: location-v1.yaml: ln 23:  [MANDATORY]    [cod-design]      New code types SHOULD be represented as string values in lowerCamelCase.       /components/schemas/BelgianRegionCode
```

OpenAPI document:

```yaml
BelgianRegionCode:
  description: Belgian Region represented by an ISO 3166-2:BE code
  x-ignore-rules:
    "cod-design": Exempt existing (ISO) code from lowerCamelCase rule 
    #"rule identifier": "motivation for ignoring the rule"  (rule identifier can be found in violiation error message)
  type: string
  enum: 
    - BE-BRU
    - BE-WAL
    - BE-VLG
```

### Exclude an external OpenApi file
The excludedFiles maven parameter can be used to exclude certain imported openapi files.
The intended use is to exclude validation of imported openapi files which are conform Belgif standards and are not under the developers influence to modify.
Wildcards could be used.
Example:
```xml
<plugins>
  <plugin>
    <groupId>io.github.belgif.rest.guide.validator</groupId>
    <artifactId>belgif-rest-guide-validator-maven-plugin</artifactId>
    <version>2.0.0</version> <!-- update this to the latest version -->
    <executions>
      <execution>
        <goals>
          <goal>api-validator</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <files>
        <file>openapi.yaml</file>
      </files>
      <excludedFiles>
        <excludedFile>schemas/belgif/**</excludedFile>
        <excludedFile>**/logo.yaml</excludedFile>
      </excludedFiles>
    </configuration>
  </plugin>
</plugins>
```

### Output
The plugin can use the following options to write the validation error:
* CONSOLE: default option. Write to the console.
* JUNIT: generate a junit xml file.
* LOG4J: use log4j.
* NONE: no output.
```xml
<plugins>
    <plugin>
        <groupId>io.github.belgif.rest.guide.validator</groupId>
        <artifactId>belgif-rest-guide-validator-maven-plugin</artifactId>
        <version>2.0.0</version> <!-- update this to the latest version -->
        <executions>
            <execution>
                <goals>
                    <goal>api-validator</goal>
                </goals>
            </execution>
        </executions>
        <configuration>            
            <files>
                <file>target/classes/</file> <!-- all yaml and json files in this folder-->
            </files>
            <outputTypes>
                <outputType>JUNIT</outputType>
                <outputType>CONSOLE</outputType>
            </outputTypes>
        </configuration>
    </plugin>
</plugins>
```
### skipOnErrors
You can execute the plugin to validate the api files without breaking the build in case of validation errors with the 'skipOnErrors' parameter.

```xml
<plugins>
    <plugin>
        <groupId>io.github.belgif.rest.guide.validator</groupId>
        <artifactId>belgif-rest-guide-validator-maven-plugin</artifactId>
        <version>2.0.0</version> <!-- update this to the latest version -->
        <executions>
            <execution>
                <goals>
                    <goal>api-validator</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <skipOnErrors>true</skipOnErrors>
            <files>
                <file>target/classes/</file> 
            </files>
        </configuration>
    </plugin>
</plugins>
```
## References

| Parameter | Type | Default | Description                                                                                                          |
| --------- | ---- | ------- |----------------------------------------------------------------------------------------------------------------------|
| files | Collection of File |  | file or folder. For a folder all json and yaml files will be used.                                                   |
| excludedFiles | Collection of File | | File(s) or folder(s) to exclude from validation. Use of wildcards is possible.                                       | 
| fileWithExclusions | Collection of FileWithExclusion |  | _obsoleted_ IGNORED a file and a collection of excludesPath. `x-ignore-rules` should be used instead.                |
| excludeResources | Collection of String | | _obsoleted_ IGNORED paths in the API to exclude from the validation for all files. `x-ignore-rules` should be used instead. |
| skipOnErrors | boolean | false | Parameter to avoid maven fail in case of validation error.                                                           |
| outputTypes | OutputType | CONSOLE | Output processors. The value can be: CONSOLE, JUNIT, JUNIT2, JUNIT3, LOG4J, NONE                                     |
| outputDir | File | target/ | Output directory for the junit report file (JUNIT outputType)                                                        |
| groupBy   | rule / file | rule | Specify how you want to group the violation output |
