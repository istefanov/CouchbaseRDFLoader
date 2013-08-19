CouchbaseRDFLoader
==================

This is part of the paper "NoSQL Databases for RDF: An Empirical Evaluation". It takes care of the conversion of RDF triples into JSON format and loads the data into Couchbase.

###Data sets

For the paper we have used 3 data sets:
 - BSBM 10M
 - BSBM 100M
 - DBpedia 100M

You can generate the Berlin Benchmark datasets as described [here](http://wifo5-03.informatik.uni-mannheim.de/bizer/berlinsparqlbenchmark/spec/BenchmarkRules/index.html#datagenerator). 
The DBpedia datasets can be downloaded from [here](http://dbpedia.aksw.org/benchmark.dbpedia.org/).
The datasets are also available on the public Amazon Machine Image (AMI) with name: NoSQL-RDF-Couchbase, 
which is loaded with data sets and runnable jars, so our experiemtns can be easily rerun.

###Couchbase
To use the application you need to setup and run a Couchbase database server: http://www.couchbase.com/

###Running

This application reads an N-triples data set and converts it into JSON documents. 
Each document has as an ID the subject of all triples bearing the same subject (a molecule). 
Each document with such an ID consists of two JSON arrays, first one with the predicates, 
second one with the objects from the triples.

When the triples are loaded into Couchbase we need to generate 3 indices (views). 
The javascript code for the views is located in the views.txt document. 
When creating the views in Couchbase, 
locate each in a separate design document bearing the same name as the view indicated in the views.txt file. 
The views creation will take some time up to some hours depending on the cluster configuration and available resources.

After finishing loading and generating the views, you can proceed to qurying the data with the help of [CouchbaseRDF](https://github.com/istefanov/CouchbaseRDF).
