# iPOJO Nature

iPOJO Nature is a plug-in for Eclipse (3.6+) that applies the
[iPOJO](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html)
manipulation as soon as the JDT compiler as done his job.


## Installation

* Move to the `trunk/` directory

* Compile with [Maven](https://maven.apache.org/download.cgi):
`mvn clean verify`

* Install the plug-in via Eclipse, looking for:
  * the "local" update site at `trunk/update-site/target/repository`
  * the "archive" update site at `trunk/update-site/target/org.ow2.chameleon.eclipse.ipojo.updatesite-<version>.zip`


## License

iPOJO Nature is licensed under the Apache License 2.0.
