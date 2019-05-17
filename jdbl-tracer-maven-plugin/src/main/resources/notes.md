Get the list of classes loaded by the JVM after executing a jar

```bash
java -verbose:class -jar target/clitools-1.0.0-SNAPSHOT-jar-with-dependencies.jar whoami | cut -d ' ' -f2 | grep -v "java\\." | grep -v "sun\\." | grep -v "jdk\\." | grep "\\." | sort
```
