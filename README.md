Introduction
============

Münchhausen is a utility for bootstrapping Java programs.

Java programs often need a bunch of JAR-files in classpath in order to run,
but the startup scripts for finding the dependencies and creating proper
classpath might become quite complex and are not portable between different
systems. Münchhausen simplifies this procedure: it scans given directory
for JAR-files, constructs a ClassLoader that includes the archives and then
calls the main class of the application.

Münchhausen is dedicated to Baron Münchhausen who, according to a German
legend, was able to lift himself out of a swamp by pulling himself up by
his bootstraps.

Basic usage
===========

To start an application whose main-class is "foo.FooMain" and whose JAR files
are found somewhere in the directory tree rooted at current directory, you
can simply say:

    java -Dbootstrap.mainclass=foo.FooMain -jar munchhausen-x.y.jar <arguments>

The \<arguments\> are passed directly to application's main method.

Configuration properties
========================

The following system properties can be used to customize the functionality:

  * _bootstrap.mainclass_     The main class of the application. (Required.)
  * _bootstrap.libdir_        Root-directory for scanned JAR-files. (Default is current directory.)
  * _bootstrap.resourcedir_   Additional directory added to classpath. (Optional.)
