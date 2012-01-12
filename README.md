# log2json

Log2json is small utility written in clojure.

## Usage

Log2json utility used to parse tomcat access log files and generate json file each for configured module.
The JSON data format is as per Geckoboard SaaS, which used to generate the charts. The following types
of charts used to view the module access statistics.<br/>
1. Bar chart - to view the errors. <br/>
2. Line chart - to view the last eight hours successful access statistics.<br/>
3. Line chart - to view the last eight hours error statistics.<br/>


## License

Copyright (C) 2011 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
