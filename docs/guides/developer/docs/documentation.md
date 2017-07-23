Code Style
==========

Maven will automatically check for coding style violations for Java and interrupt the build process if any are found,
displaying a message about it. Apart from that, here are some general rules:

### Everything:

- UTF-8 encoded

### Java, HTML and JavaScript:

- Indentation is done with exactly two spaces
- No line of code should be wider than 120 columns
- No trailing spaces
- Avoid unnecessary code style changes

### Markdown:

- Avoid lines wider than 120 columns
- Avoid trailing spaces
- Avoid unnecessary style changes

### Everything else:

- Try applying the Java style rules
- If in doubt, ask on list


## Logging Rules

The following is a list of logging levels and their use.

Level | Description
------|---------------------------------------------------------------------------
TRACE | Information that would only be useful when debugging a specific subsystem.
DEBUG | Information relevant to development. Used to provide detailed information for developers
INFO  | Information relevant to server administrators.  Creating, updating, and deleting files should be logged here.
WARN  | Handled exceptions. A warning should be logged any time there is a problem handling a user action or background process.
ERROR | Unhandled exceptions. Any problem with an Opencast system that was not automatically handled.  This should be things like components failing to activate correctly.


Documentation
=============

One important measurement for code quality is its documentation. This is especially the case for open source projects
that count on the support of external developers and a strong community around the code base.  With that in mind, here
are some of the goals you should keep in mind when working within the Opencast codebase.

### Package Level Documentation

Package documentation should cover the general concepts that are being used throughout the package, along with the 
principal interfaces and classes that are being used.  A good example is provided by the [Java 6 sql package description](http://java.sun.com/javase/6/docs/api/javax/sql/package-summary.html#package_description).

#### Where does the documentation go?

The package information should be put into a file called `package-info.java` and reside in the root of the package.
Generally, the package-info is a java class itself consisting of documentation and package declaration only.  Documentation
can be found [here](http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#packagecomment).

#### Checklist

In order to achieve decent package documentation, make sure your documentation answers the following questions:

- What problem domain does your package deal with?
- Are there any concepts that the developer should understand?
- What are the different strategies that your package might implement?
- What are the entry points into your package in terms of interfaces and classes?
- What best practices are there to use the package?
- What major requirements need to be met before being able to use the package?
- Is there related external documentation (rfc, specifications) that should be linked?

### Classes

By looking at the documentation of a class or an interface, the devloper should have a clear idea as to how the class can be used, what pitfalls
there might be in terms of threading, performance etc. Again, a good example can be found in the [Url class](http://java.sun.com/javase/6/docs/api/java/net/URL.html)
of the Java Development Kit.

#### Documentation body

- Has a one-sentence description been given on what the class does or describe?
- Is there a description of concepts that should be known to users of the class?
- Are there possible pitfalls in the area of threading, transactions or performance?
- Are any other issues covered that might arise when using the class?
- Have any constraints be documented, like special behaviour of the equals() method?Are best practices on how to use the class documented?

#### Tags

- Are there related classes or external resources that should be linked using @see ?

### Methods

As with type documentation, the notes on a method should consist of instructions on what to expect in terms of behaviour when the method is
called. Also, the user should be informed about any constraints that might apply to the objects state, the format of input parameters etc. As an
example, take a look at the [constructor for the URL class](http://java.sun.com/javase/6/docs/api/java/net/URL.html#URL(java.lang.String,%20java.lang.String,%20int,%20java.lang.String))
of the Java Development Kit.

#### Documentation body

- In general, what does the method do?
- Are there any requirements regarding the format of the input parameters?
- What does the method return with respect to input parameter values or state?
- If applicable, are there any constraints regarding an objects state?
- How are special parameter values (e. g. null ) handled?
- Does the user know what exceptions are thrown under which circumstances?

#### Tags

- Are the input parameters documented using the @param tag?
- Is the return value documented using the @return tag?
- Are there related methods that should be linked using @see ?
- Are the exceptions documented with @throws ?

### REST Endpoints

All Opencast REST endpoints must display HTML formatted documentation at the /docs path.

Please be sure that your documentation includes:

- An entry for each path pattern, categorized as read and write paths.
- Descriptions for each path pattern, including:
    - the HTTP method (GET, POST, PUT, DELETE)
    - Required and optional parameters
    - the expected input, including data formats and headers
    - the expected output, including data formats, headers, and HTTP response codes

