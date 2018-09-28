# ABySS-explorer
visual exploration of ABySS assembly data

Written by [Ka Ming Nip](mailto:kmnip@bcgsc.ca)

Copyright 2011 Canada's Michael Smith Genome Sciences Centre, BC Cancer

-------------------------------------------------------------------------------

## Dependency :pushpin:

* [Java SE Runtime Environment (JRE) 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)

### Libraries used (already included in the compiled package):

* [Apache Batik SVG Toolkit](https://xmlgraphics.apache.org/batik/)
* [JFreeChart 1.0.18](http://www.jfree.org/jfreechart/)
* [JUNG 2.0.1](http://jung.sourceforge.net/)

Check your Java version:
```
java -version
```

Example:
```
java version "1.8.0_101"
Java(TM) SE Runtime Environment (build 1.8.0_101-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.101-b13, mixed mode)
```

## Installation :wrench:

1. Download the binary tarball `rnabloom_vX.X.X.tar.gz` from the [releases](https://github.com/bcgsc/RNA-Bloom/releases) section.
2. Extract the downloaded tarball with the command:
```
tar -zxf ABySS-explorer-X.X.X.tar.gz
```
3. ABySS-explorer is ready to use, ie. `java -jar /path/to/ABySS-explorer.jar ...`

**There is nothing to compile/configure/build!**

If you decide to move the files to another location, make sure `ABySS-explorer.jar` and `lib/` are under the same directory.

## Java Options :coffee:

### To limit the size of Java heap to 1GB:
```
java -Xmx1g -jar ABySS-explorer.jar ...
```

[Other JVM options](https://docs.oracle.com/cd/E37116_01/install.111210/e23737/configuring_jvm.htm#OUDIG00071) may also be used.

## User Manual :orange_book:

Our user manual is available for [download as a PDF file](http://www.bcgsc.ca/downloads/abyss-explorer/abyss-explorer-1.3.0/v1_3_0_user_manual.pdf).
The current version is almost identical to version 1.3.x. An updated user manual for the current version will be made available.

## Citation :pencil2:

If you use ABySS-explorer for your research, please cite:

Nielsen CB, Jackman SD, Birol I, Jones SJ. ABySS-Explorer: visualizing genome sequence assemblies. IEEE Trans Vis Comput Graph. 2009 Nov-Dec;15(6):881-8. doi: [10.1109/TVCG.2009.116.](https://doi.org/10.1109/TVCG.2009.116)

--------------------------------------------------------------------------------