# belgif-rest-guide-validator plugin

The belgif-rest-guide-validator Maven plugin is used to validate if an OpenAPI document conforms to the guidelines in the [Belgif REST guide](https://www.belgif.be/specification/rest/api-guide/).

# Prerequisites
This Maven plugin requires JDK 17 or higher to be used in the Maven runtime. 
Note that the JDK version used to compile the source code of the project can differ. 
IDEs may need manual configuration to set the appropriate Maven runtime JDK version.
The minimum maven version is 3.8.5

## Goal Overview
The goal `api-validator` binds by default to the lifecycle phase `prepare-package`.
The goal `validate-openapi` can be used to validate an OpenAPI document outside a Maven project build execution.

## Usage

The plugin will output violations against rules. You can look up the rule identifiers (e.g. [uri-notat]) in the [Belgif REST guide](https://www.belgif.be/specification/rest/api-guide/) for more information and examples.
Referenced external OpenAPI documents (from `$ref` properties) will be validated as well, unless explicitly excluded in the plugin's configuration.
Only OpenAPI documents on the local file system are currently supported.

There are two ways to run the validator:

a) Run the validation during each Maven build, by adding the plugin in your project's pom.xml file (see example below).
The validation report will be in the console in the maven build output.

b) Run the validation once

Navigate to the directory containing the OpenAPI file, and launch from command line:  
```
   mvn io.github.belgif.rest.guide.validator:belgif-rest-guide-validator-maven-plugin:2.0.0:validate-openapi "-Dapi-validator.files=openapi.yaml"
```
(change openapi.yaml to the name of your OpenAPI file)
Check the result in the console output of the maven execution.

### Example output
The console output is grouped by rule (default) or grouped by file.
Each rule shows:
* Violation level (MANDATORY / RECOMMENDED / STYLE / IGNORED)
* Rule name ([rule-name])
* A message that describes the general issue of the rule
* The number of occurences

Then for each violation of that rule the console output contains:
* filename + line number
* A JsonPointer to the exact location of the violation
* In some cases, additional information about this specific violation.

```
 OpenApi validation summary: 10 violations and 0 ignored violations.
[MANDATORY]    [cod-design] New code types SHOULD be represented as string values in lowerCamelCase. 1 occurrence:
logo.yaml       ln  15  #/components/schemas/LogoMetaData/properties/mediaType
[MANDATORY]    [err-problem] Each error response of each operation SHOULD have a media type "application/problem+json" 1 occurrence:
openapi.yaml    ln 518  #/paths/health/get/responses/500 -- [Operation: GET /health]
[MANDATORY]    [evo-object] In a request or response body, if any, you MUST always return a JSON object (and not e.g. an array) as a top level data structure to support future extensibility.  5 occurrences:
openapi.yaml    ln 156  #/paths/organizations/post/requestBody/content/application/json
openapi.yaml    ln 187  #/paths/organizations/{enterpriseNumber}/get/responses/200/content/application/json
openapi.yaml    ln 223  #/paths/organizations/{enterpriseNumber}/put/requestBody/content/application/json
openapi.yaml    ln 232  #/paths/organizations/{enterpriseNumber}/put/responses/200/content/application/json
openapi.yaml    ln 262  #/paths/organizations/{enterpriseNumber}/patch/responses/200/content/application/json
[MANDATORY]    [hdr-case]   By convention, HTTP headers SHOULD use Kebab-Case with uppercase for readability and consistency.  1 occurrence:
openapi.yaml    ln 365  #/paths/logos/{id}/get/parameters/1
[MANDATORY]    [jsn-naming] All JSON property names SHOULD be written in lowerCamelCase notation. 1 occurrence:
logo.yaml       ln  24  #/components/schemas/Logo -- [propertyName: Image]
[MANDATORY]    [oas-exampl] Example does not validate against schema 1 occurrence:
employer.yaml   ln  40  #/components/schemas/Employer/example
-- employerId: Type expected 'integer', found 'string'. In Schema: employer.yaml#/components/schemas/Employer : <belgif/employment/identifier/v1/employment-identifier-v1.yaml#/components/schemas/EmployerId>.<type>
-- employerId: Value '164893015' does not match format 'int64'. In Schema: employer.yaml#/components/schemas/Employer : <belgif/employment/identifier/v1/employment-identifier-v1.yaml#/components/schemas/EmployerId>.<format>
```

### Basic example
The following example demonstrates a basic plugin configuration for validating an OpenAPI document.
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
