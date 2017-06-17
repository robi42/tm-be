TempMunger
==========

Backend
-------

Ensure having JDK 8 installed.

Get it here:<br>
<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>

Then, for instance:

    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`

To build & run via command line shell:

    ./gradlew clean build && java -jar build/libs/temp-munger.jar

Or simply run/debug `Application.main()` via IDE(A).

An endpoint to play with:

    curl -iH 'Content-Type: application/json' 'localhost:8888/temporal-entries/search?from=20' -d '
        {"aggs": {"agg": {"terms": {"field": "Jahr"}}}}
    ' | python -mjson.tool

Admin endpoints, powered by Spring Boot, reside at:<br>
`/manage/*` on port `8889` (auth-protected)<br>
Docs, incl. ones for REST API, generated via Asciidoctor, at:<br>
`/docs`

This backend embraces Kotlin, Gradle, and Spring Boot.<br>
Moreover, it's built upon Elasticsearch and Apache Spark.

Disclaimer: please keep in mind, it's a prototype.<br>
So, e.g., ES & Spark wouldn't be run embedded IRL.<br>
Test datasets are available via public domain...
