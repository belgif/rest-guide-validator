# Introduction

The belgif-rest-guide-validator plugin is used to validate a Swagger API to conforms the belgif standards.

## Overview

The project is based on the rules engine Drools.

## Maven Modules

### belgif-rest-guide-validator-master
Common parent hold common dependencies version and common plugins.

### belgif-rest-guide-validator-core

The core module provide the functions to manipulate the java representation of an open-api specification and the classes related to the violation.
It provide also a output abstraction and his basic implementations.

### belgif-rest-guide-validator-rules

Contains all Drools rules and the related tests.
Rules module depends on the core module .

### belgif-rest-guide-validator-maven-plugin

The maven-plugin build the plugin. It provides the functionalities to manage the parameters.
It call the drools engine.
Maven-plugin module depends on the rules module.

### integrationtest

Use invoker maven plugin to test the belgif-rest-guide-validator-maven-plugin with some maven projects.

## Main Dependencies

* io.swagger.parser.v3:swagger-parser.  
Parse a json/swager file and build a java object structure.

* com.jayway.jsonpath:json-path.  
Read a json file and use json path.

* org.drools:*, org.kie:*  
Rules engine.

* org.apache.maven:*  
Maven plugin

# Implementation details

## Use rules Engine
 
> Build a shared *classpath* KieContainer (static)
>> KieServices kieServices = KieServices.Factory.get();  
>> KieContainer kContainer = kieServices.getKieClasspathContainer();  

> For each file call the rule engine   
> *OpenApiValidator.callRuleOAS(*OpenApiViolationAggregator* **oas**, *OpenAPI* **openApi**)*
>> var kSession = kContainer.newStatelessKieSession();  *// create a stateless session*
>> kSession.setGlobal("oas", **oas**);  *// set the global*
>> kSession.execute(**openApi**);  *// call the rule engine*

## Rules (drl file)
> **rule** "the rule name"   
> **when**  
> *$api* : OpenAPI()  
> *$operationId* : String() from ApiFunctions.getOperationId(*$api*, OperationEnum.GET, "201")  
> **then**  
> violationGet(oas, *$operationId*, "201");  
> **end**

 For this sentence ' **myVar**: String() **from** *collection* '   
drool will apply the ***then*** section for each element in collection by using **myVar**


