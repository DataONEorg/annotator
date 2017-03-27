annotator
=========

Annotation services and utilities for the DataONE project.

Design documentation is in the [sem-prov-design](https://github.com/DataONEorg/sem-prov-design) repository.

AnnotatorJS REST
----------------
This project includes an implementation of the AnnotatorJS REST API for saving, searching, and retrieving annotations that are ultimately stored 
in a DataONE Member Node. The project should be included in a separate Maven webapp project that initializes and defines the paths 
to the AnnotatorRestServlet. The `annotator.nodeid` field in the annotator.properties file defines which MN will serve as the storage repository
for annotations. Note that the MN needs to be registered in the same environment as defined by `D1Client.CN_URL` property.

A sample web.xml entry for enabling the servlet:

	<!--  AnnotatorStore proxy to MN storage -->
	<servlet>
		<servlet-name>annotatorStoreRestServlet</servlet-name>
		<servlet-class>org.dataone.annotator.store.AnnotatorRestServlet</servlet-class>
	</servlet>
	<servlet-mapping>
		<servlet-name>annotatorStoreRestServlet</servlet-name>
		<url-pattern>/annotator/*</url-pattern>
	</servlet-mapping>

For details on the AnnotatorJS API, see [their documentation page](http://docs.annotatorjs.org/en/v1.2.x/storage.html)


Annotation Generation
---------------------
This project also includes a command-line utility for generating annotations based on recommendations from various "matcher" services.
These include: Bioportal, ESOR, and the newer ESOR Cosine service. There is also a "matcher" that relies only on manually provided annotations 
from a spreadsheet (no external service is called to fid concepts since they are provided along with the PID/entity/attribute being annotated).

Only one `annotator.matcher.className` should be configured at any given time. Similarly, only one `annotator.store.className` should be defined. 
For testing, a MockAnnotatorStore can be used and will only print the annotations to stdout for inspection.

After configuring the annotator.properties as desired, the utility can be called:

	mvn exec:java -Dexec.mainClass="org.dataone.annotator.Annotator" -Dexec.args="-create -pidfile <URL to list of pids>"

There are other flag options as well, described below.

For automated annotations:
* `create` (as above)
* `createAll` (does not use pidfile and simply generates annotations for EVERY metadata object in the configured repo)
* `remove` (removes annotations for pids in the file)
* `removeAll` (removes all annotations in the repo)

For other actions:
* `manual` (generates annotations from manual spreadsheet identified by file argument)
* `types` (generates ontology concepts provided in the file argument)

Development logs
----------------
For developers interested in getting an email for each push to the sem-prov repositories, you can subscribe to our mailing list:

* sem-prov-dev: [List Info](http://lists.dataone.org/mailman/listinfo/sem-prov-dev/)
