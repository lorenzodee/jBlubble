# jBlubble Sample

This sample uses jblubble-api and jblubble-jdbc. If you don't have them installed yet, install them first by:

	> cd jblubble-api
	> mvn install
	> cd jblubble-jdbc
	> mvn install

To run the sample:

	> mvn tomcat7:run

Then go to [http://localhost:8080/jblubble-sample/uploads](http://localhost:8080/jblubble-sample/uploads) which is handled by `UploadServlet`. You can also go to [http://localhost:8080/jblubble-sample/persons](http://localhost:8080/jblubble-sample/persons) which is handled by a Spring MVC controller &mdash; `PersonController` and uses JPA. There you can see how a BLOB is referenced by key. A JPA `AttributeConverter` was used to convert `BlobKey`.

