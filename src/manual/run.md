## Running tests

Citrus test cases are nothing but Java classes that get executed within a Java runtime environment. Each Citrus test therefore relates 
to a Java class representing a JUnit or TestNG unit test. As optional add on a Citrus test can have a XML test declaration file. This is for 
those of you that do not want to code in Java. In this case the XML part holds all actions to tell Citrus what should happen in the test case.
The Java part will then just be responsible for test execution and is not likely to be changed at all. In the following sections we concentrate 
on the Java part and the test execution mechanism.

If you create new test cases in Citrus - for instance via Maven plugin or ANT build script - Citrus 
generates both parts in your test directory. For example: if you create a new test named 
**MyFirstCitrusTest** you will find these two files as a result:

```
src/it/tests/com/consol/citrus/MyFirstCitrusTest.xml
src/it/java/com/consol/citrus/MyFirstCitrusTest.java
```

**Note**
If you prefer to just write Java code you can throw away the XML part immediately and continue working with the Java part only. In case
you are familiar with writing Java code you may just skip the test template generation via Maven or ANT and preferably just create new Citrus Java 
test classes on your own.


With the creation of this test we have already made a very important decision. During creation, Citrus asks 
you which execution framework should be used for this test. There are basically three options available: 
***testng*** and ***junit***.

So why is Citrus related to Unit tests although it is intended to be a framework for integration testing? 
The answer to this question is quite simple: This is because Citrus wants to benefit from both JUnit and TestNG for 
Java test execution. Both the JUnit and TestNG Java APIs offer various ways of execution and both frameworks are widely supported 
by other tools (e.g. continuous build, build lifecycle, development IDE).

Users might already know one of these frameworks and the chances are good that they are familiar with at least one of them. 
Everything you can do with JUnit and TestNG test cases you can do with Citrus tests also. Include them into your Maven build 
lifecycle. Execute tests from your IDE (Eclipse, IDEA or NetBeans). Include them into a continuous build tool (e.g. Jenkins).
Generate test execution reports and test coverage reports with Sonar, Cobertura and so on. The possibilities with JUnit and TestNG are amazing.

So let us have a closer look at the Citrus TestNG and JUnit integration.

### Run with TestNG

TestNG stands for next generation testing and has had a great influence in adding Java annotations to the unit
test community. Citrus is able to generate TestNG Java classes that are executable as test cases. See the following standard
template that Citrus will generate when having new test cases:

```java
package com.consol.citrus.samples;

import org.testng.annotations.Test;

import com.consol.citrus.annotations.CitrusXmlTest;
import com.consol.citrus.testng.AbstractTestNGCitrusTest;

/**
 * TODO: Description
 *
 * @author Unknown
 */
@Test
public class SampleIT extends AbstractTestNGCitrusTest {
    @CitrusXmlTest(name = "SampleIT")
    public void sampleTest() {}
}
```

If you are familiar with TestNG you will see that the generated Java class is nothing but a normal TestNG test class.
We just extend a basic Citrus TestNG class which enables the Citrus test execution features for us. Besides that we have a usual TestNG
**@Test** annotation placed on our class so all methods inside the class will be executed as separate test case.

The good news is that we can still use the fantastic TestNG features in our test class. You can think of parallel test execution, test groups,
setup and tear down operations and so on. Just to give an example we can simply add a test group to our test like this:

```
@Test(groups = {"long-running"})
```

For more information on TestNG please visit the official homepage, where you find a complete reference documentation.

You might have noticed that the example above loads test cases from XML. This is why we are using the **@CitrusXmlTest** annotation. Again
this approach is for people that want to write no Java code. The test logic is then provided in the XML test definition. We discuss XML tests in Citrus in more detail in
[run-xml-tests](run-xml-tests.md). Next lets have a look at a TestNG Java DSL test.

When writing tests in pure Java we have pretty much the exact same logic that applies to executing Citrus test cases. The Citrus test extends from a TestNG base class and uses
the normal **@Test** annotations on method or class level. Here is a short sample TestNG Java class for this:

```java
import org.testng.annotations.Test;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;

@Test
public class MyFirstTestDesigner extends TestNGCitrusTestDesigner {
    @CitrusTest(name = "MyFirstIT")
    public void myFirstTest() {
        description("First example showing the basic test case definition elements!");

        variable("text", "Hello Test Framework");

        echo("${test}");
    }
}
```

You see the class is quite similar to the XML test variation. Now we extend a Citrus test designer class which enables the Java DSL features in addition to the TestNG
test execution for us. The basic **@Test** annotation for TestNG has not changed. We still have a usual TestNG class with the possibility of several methods
each representing a separate unit test.

Now what has changed is the **@CitrusTest** annotation. Now the Citrus test logic is placed directly as the method body with using the
Java domain specific language features. The XML Citrus test part is not necessary anymore. If you are wondering about the designer super class and the Java DSL methods for adding
the test logic to your test please be patient we will learn more about the Java DSL features in this reference guide later on.

Up to now we just concentrate on the TestNG integration that is quite easy isn't it.

### Using TestNG DataProviders

TestNG as a framework comes with lots of great features such as data providers. Data providers execute a test case several times. Each test execution gets
a specific parameter value. With Citrus you can use those data provider parameters inside the test as variables. See the next listing
on how to use TestNG data providers in Citrus:

```java
public class DataProviderIT extends AbstractTestNGCitrusTest {

  @CitrusXmlTest
  @CitrusParameters( "message" )
  @Test(dataProvider = "messageDataProvider")
  public void DataProviderIT(ITestContext testContext) {
  }

  @DataProvider
  public Object[][] messageDataProvider() {
  return new Object[][] {
      { "Hello World!" },
      { "Hallo Welt!" },
      { "Hi Citrus!" },
    };
  }
}
```

Above test case method is annotated with TestNG data provider called **messageDataProvider**. In the same class you can write the data provider
that returns a list of parameter values. TestNG will execute the test case several times according to the provided parameter list. Each
execution is shipped with the respective parameter value. According to the **@CitrusParameter** annotation the test will have a test variable
called **message** that is accessible as usual.

### Run with JUnit

JUnit is a very popular unit test framework for Java applications widely accepted and widely supported by many tools. In general Citrus supports both JUnit and TestNG
as test execution frameworks. Although the TestNG customization features are slightly more powerful than those offered by JUnit you as a Citrus user should be able to use the framework of
your choice. The complete support for executing test cases with package scans and multiple annotated methods is given for both frameworks. If you choose **junit** as execution framework
Citrus generates a Java file that looks like this:

```java
package com.consol.citrus.samples;

import org.junit.Test;
import com.consol.citrus.annotations.CitrusXmlTest;
import com.consol.citrus.junit.AbstractJUnit4CitrusTest;

/**
 * TODO: Description
 *
 * @author Unknown
 */
public class SampleIT extends AbstractJUnit4CitrusTest {
    @Test
    @CitrusXmlTest(name = "SampleIT")
    public void sampleTest() {}
}
```

JUnit and TestNG as frameworks reveal slight differences, but the idea is the same. We extend a base JUnit Citrus test class and have one to many test methods that load the XML
Citrus test cases for execution. As you can see the test class can hold several annotated test methods that get executed as JUnit tests. The fine thing here is that we are still able
to use all JUnit features such as before/after test actions or enable/disable tests.

The Java JUnit classes are simply responsible for loading and executing the Citrus test cases. Citrus takes care on loading the XML test as a file system resource and to set up the
Spring application context. The test is executed and success/failure state is reported exactly like a usual JUnit unit test would do. This also means that you can execute this Citrus JUnit class like every other
JUnit test, especially out of any Java IDE, with Maven, with ANT and so on. This means that you can easily include the Citrus test execution into
you software building lifecycle and continuous build.

**Tip**
So now we know both TestNG and JUnit support in Citrus. Which framework should someone choose?
To be honest, there is no easy answer to this question. The basic features are equivalent,
but TestNG offers better possibilities for designing more complex test setup with test groups and
tasks before and after a group of tests. This is why TestNG is the default option in Citrus. But in
the end you have to decide on your own which framework fits best for your project.

The first example seen here is using **@CitrusXmlTest** annotation in order to load a XML file as test. The Java part is then just an empty envelope for executing the test with JUnit. This approach is for those
of you that are not familiar with Java at all. You can find more information on loading XML files as Citrus tests in [run-xml-tests](run-xml-tests.md). Secondly of course we also have the possibility to use the Citrus Java DSL
with JUnit. See the following example on how this looks like:

```java
package com.consol.citrus.samples;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.JUnit4CitrusTestDesigner;
import org.junit.Test;

/**
 * TODO: Description
 *
 * @author Unknown
 */
public class SampleIT extends JUnit4CitrusTestDesigner {

    @Test
    @CitrusTest
    public void EchoSampleIT() {
        variable("time", "citrus:currentDate()");
        echo("Hello Citrus!");
        echo("CurrentTime is: ${time}");
    }

    @Test
    @CitrusTest(name = "EchoIT")
    public void echoTest() {
        echo("Hello Citrus!");
    }
}
```

The Java DSL test case looks quite familiar as we also use the JUnit4 **@Test** annotation in order
to mark our test for unit test execution. In addition to that we add a **@CitrusTest** annotation and extend
from a basic JUnit4 Citrus test designer which enables the Java domain specific language features. The Citrus test logic goes
directly to the method block. There is no need for a XML test file anymore.

As you can see the **@CitrusTest** annotation supports multiple test methods in one single class. Each test is
prepared and executed separately just as you know it from JUnit. You can define an explicit Citrus test name that is used in Citrus test
reports. If no explicit test name is given the test method name will be used as a test name.

If you need to know more details about the test designer and on how to use the Citrus Java DSL just continue with this reference guide. We will describe
the capabilities in detail later on.

### Running XML tests

Now we also use the @CitrusXmlTest annotation in the Java class. This annotation makes Citrus search for a XML file that represents
the Citrus test within your classpath. Later on we will also discuss another Citrus annotation (**@CitrusTest**) which
stands for defining the Citrus test just with Java domain specific language features. For now we continue to deal with the XML Citrus test execution.

The default naming convention requires a XML file with the tests name in the same package that the
Java class is placed in. In the basic example above this means that Citrus searches for a XML test file in
**com/consol/citrus/samples/SampleIT.xml**. You tell Citrus to search for another XML file by using the @CitrusXmlTest
annotation properties. Following annotation properties are valid:

* **name**: List of test case names to execute. Names also define XML file names to look for (**.xml** file extension is not needed here).
* **packageName**: Custom package location for the XML files to load
* **packageScan**: List of packages that are automatically scanned for XML test files to execute. For each XML file found separate
test is executed. Note that this performs a Java Classpath package scan so all XML files in package are assumed to be valid Citrus XML
test cases. In order to minimize the amount of accidentally loaded XML files the scan will only load XML files with
**/*Test.xml and **/*IT.xml file name pattern.

You can also mix the various CitrusXmlTest annotation patterns in a single Java class. So we are able to have several test cases in one
single Java class. Each annotated method represents one or more Citrus XML test cases. Se the following example to see what this is about.

```java
@Test
public class SampleIT extends AbstractTestNGCitrusTest {
    @CitrusXmlTest(name = "SampleIT")
    public void sampleTest() {}

    @CitrusXmlTest(name = { "SampleIT", "AnotherIT" })
    public void multipleTests() {}

    @CitrusXmlTest(name = "OtherIT", packageName = "com.other.testpackage")
    public void otherPackageTest() {}

    @CitrusXmlTest(packageScan =  { "com.some.testpackage", "com.other.testpackage" })
    public void packageScanTest() {}
}
```

You are free to combine these test annotations as you like in your class. As the whole Java class is annotated with the TestNG
**@Test** annotation each method gets executed automatically. Citrus will also take care on executing each XML test case as
a separate unit test. So the test reports will have the exact number of executed tests and the JUnit/TestNG test reports do have the exact test
outline for further usage (e.g. in continuous build reports).

**Note**
When test execution takes place each test method annotation is evaluated in sequence. XML test cases that match several times, for instance by
explicit name reference and a package scan will be executed several times respectively.

The best thing about using the **@CitrusXmlTest** annotation is that you can continue to use the fabulous TestNG
capabilities (e.g. test groups, invocation count, thread pools, data providers, and so on).

So now we have seen how to execute a Citrus XML test with TestNG.