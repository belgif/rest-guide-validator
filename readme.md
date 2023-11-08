# Rest-styleguide-validation plugin

The rest-styleguide-validation Plugin is used to validate a Swagger API to conforms the Belgif standards (https://www.belgif.be/specification/rest/api-guide/).

*Note: the services-rest-parent use this plugin and the result is visible in Jenkins Test results*
## Goal Overview
The goal api-validator has as default phase the LifecyclePhase.PREPARE_PACKAGE.

## Usage
The plugin is commonly used to verify the openApi files for a project.

### Basic example
The following example demonstrates a basic plugin configuration for validating one design first api file.
```xml
<plugins>
    <plugin>
        <groupId>be.belgium.gcloud.rest</groupId>
        <artifactId>rest-styleguide-validation-maven-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
            <execution>
                <goals>
                    <goal>api-validator</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <files>
                <file>src/main/resources/swagger.yaml</file>
            </files>
        </configuration>
    </plugin>
</plugins>
```
You can now build you package as usual with:
```bash
mvn package  
```
### Folder with global exclusions
With the following configuration, the plugin will check all files in a folder.
It 'll ignore the validation issues for the paths specified in 'excludeResources'.

```xml
<plugins>
    <plugin>
        <groupId>be.belgium.gcloud.rest</groupId>
        <artifactId>rest-styleguide-validation-maven-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
            <execution>
                <goals>
                    <goal>api-validator</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <excludeResources>
                <excludeResource>/health</excludeResource>
                <excludeResource>/health/readinessProbe</excludeResource>
                <excludeResource>/health/livenessProbe</excludeResource>
            </excludeResources>
            <files>
                <file>target/classes/</file> 
            </files>
        </configuration>
    </plugin>
</plugins>
```
### Exclusions in OpenApi file
The "x-ignore-rules" object can be added inside a yaml object in the openapi file to ignore this object for a specific rule or multiple rules.
Example:
```yaml
BelgianRegionCode:
      description: Belgian Region represented by an ISO 3166-2:BE code
      x-ignore-rules:
         "cod-design": Exempt existing (ISO) code from lowerCamelCase rule
      type: string
      enum: 
        - BE-BRU
        - BE-WAL
        - BE-VLG
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
        <groupId>be.belgium.gcloud.rest</groupId>
        <artifactId>rest-styleguide-validation-maven-plugin</artifactId>
        <version>1.0.0</version>
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
        <groupId>be.belgium.gcloud.rest</groupId>
        <artifactId>rest-styleguide-validation-maven-plugin</artifactId>
        <version>1.0.0</version>
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
### File with Exclusions
In the following example '/health' will be ignored in all files. No Exception will be raised if the path doesn't exist.
The 'fileWithExclusions' is useful with the legacy apis.
```xml
<plugins>
    <plugin>
        <groupId>be.belgium.gcloud.rest</groupId>
        <artifactId>rest-styleguide-validation-maven-plugin</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <executions>
            <execution>
                <goals>
                    <goal>api-validator</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <excludeResources>
                <excludeResource>/health</excludeResource>
            </excludeResources>
            <fileWithExclusions>
                <fileWithExclusion>
                    <file>swagger-1.yaml</file>
                    <excludesPaths>
                        <excludesPath>/categories</excludesPath>
                    </excludesPaths>
                </fileWithExclusion>
                <fileWithExclusion>
                    <file>swagger-2.yaml</file>
                    <excludesPaths>
                        <excludesPath>/entity/contacts/{contactId}</excludesPath>
                        <excludesPath>/entity/contacts/{contactId}/invalidData</excludesPath>
                    </excludesPaths>
                </fileWithExclusion>
            </fileWithExclusions>
        </configuration>
    </plugin>
</plugins>
```   
## References

| Parameter | Type | Default | Description                                                                      |
| --------- | ---- | ------- |----------------------------------------------------------------------------------|
| files | Collection of File |  | file or folder. For a folder all json and yaml files will be used.               |
| fileWithExclusions | Collection of FileWithExclusion |  | a file and a collection of excludesPath                                          |
| excludeResources | Collection of String | | path to exclude from the validation for all files                                |
| skipOnErrors | boolean | false | Parameter to avoid maven fail in case of validation error.                       |
| outputTypes | OutputType | CONSOLE | Output processors. The value can be: CONSOLE, JUNIT, JUNIT2, JUNIT3, LOG4J, NONE |
| outputDir | File | target/ | Output directory for the junit report file (JUNIT outputType)                    |
