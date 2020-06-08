<img align="left" src="https://github.com/castor-software/jdbl/blob/master/logo.svg" height="100px"  alt="JDBL logo"/>


[![Build Status](https://travis-ci.org/castor-software/jdbl.svg?branch=master)](https://travis-ci.org/castor-software/jdbl)
[![PDD status](http://www.0pdd.com/svg?name=castor-software/jdbl)](http://www.0pdd.com/p?name=castor-software/jdbl)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=alert_status)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=security_rating)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)


[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=bugs)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=code_smells)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=ncloc)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=sqale_index)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=castor-software_jdbl&metric=coverage)](https://sonarcloud.io/dashboard?id=castor-software_jdbl)

<!--
[![hits of code](https://hitsofcode.com/github/castor-software/jdbl)](https://hitsofcode.com/view/github/castor-software/jdbl)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/castor-software/jdbl/blob/master/LICENSE)
-->


### What is JDBL?

JDBL is a tool for automatically specialize Java applications through dynamic and static debloat at build time. JDBL removes unused classes and methods from Maven projects (including its dependencies), as well as the Java Runtime Environment (JRE). To do so, JDBL collects execution traces by [instrumenting](https://en.wikipedia.org/wiki/Instrumentation_(computer_programming)) and transforming the bytecode on-the-fly during the distinct Maven building phases. JDBL can be used as a Maven plugin or executed out-of-the-box as a standalone Java application.

### How does it work?

JDBL runs before executing the `package` phase of the Maven build lifecycle. It detects all the types referenced in the project under analysis, as well as in its declared dependencies, at run-time. Then, JDBL removes all the unused class members (i.e., classes and methods), depending on the debloating strategy utilized.

DepClean supports three types of debloating strategies:

- **entry-point-debloat:** removes the class members that used after running the application from a given entry-point.
- **test-based-debloat:** removes the class members that are not covered by the test suite.
- **conservative-debloat:** removes the class members that are not referenced by the application, as determined statically.

The **entry-point-debloat** strategy is the most aggressive approach. In this case, the bytecode is instrumented during the Maven `compile` phase, probes are inserted in the bytecode, and the application is executed in order to collect [execution traces](https://en.wikipedia.org/wiki/Tracing_(software)). Then, the class members that were not covered are removed from the bytecode, and the transformed application is packaged as a specialized ad debloated JAR file.  

The **test-based-debloat** strategy is similar to the **entry-point**; the difference is that the execution traces are collected based on the execution of the test suite of the project.

The **conservative-debloat** strategy is the less aggressive approach. It relies on static analysis to construct a call graph of class members calls, which contains all the class members referenced by the application. Then, the members that are not referenced (a.k.a [dead code](https://en.wikipedia.org/wiki/Dead_code)) are removed from the bytecode. This approach is similar to shrinking technique performed by [Proguard](https://www.guardsquare.com/en/products/proguard), with the difference JDBL executed the debloat thorough the Maven build phases.    

Overall, JDBL produces a smaller, specialized version of the Java application without modifying its source code. The modified version is automatically packaged as a JAR file as resulting from the Maven build lifecycle.
 
## Usage

To use JDBL as a Maven plugin, first install it from source cloning this repo and running `mvn clean install`. Then, add the plugin to the `pom.xml` of the application to be debloated:

```xml
<plugin>
    <groupId>se.kth.castor</groupId>
    <artifactId>jdbl-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>${strategy}</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Where the property `${strategy}` can take one of the three debloating strategies supported by JDBL.

You also need to add the JaCoCo Maven plugin to your project:

```xml
 <plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.5</version>
    <executions>
      <execution>
          <goals>
              <goal>prepare-agent</goal>
          </goals>
      </execution>
      <execution>
          <id>report</id>
          <phase>prepare-package</phase>
          <goals>
              <goal>report</goal>
          </goals>
      </execution>
    </executions>
</plugin>
```
### Optional parameters

In the case of the **entry-point** strategy, the following additional configuration parameters are should be provided:

| Name   |  Type |   Description      | 
|:----------|:-------------:| :-------------| 
| `<entryClass>` | `<String>` | Fully qualified name of the class used as the entry point of the application. **Typical value is:** `Main`.
| `<entryMethod>` | `<String>` | Fully qualified name of the method in the `<entryClass>` used as the entry point of the application. **Typical value is:** `main`./|
| `<entryParameters>` | `Set<String>` | Parameters of the `<entryMethod>` used provided. Only string values separated by commas are permitted.
| `<skipJDBL>` | `boolean` | Skip plugin execution completely. **Default value is:** `false`.|

## License

Distributed under the MIT License. See [LICENSE](https://github.com/castor-software/jdbl/blob/master/LICENSE) for more information.
