---
title: Combining Imperative and Functional Programming
subtitle: (for building modern concurrent software)
author: Kevin Mooney
date: University of Illinois Urbana-Champaign / CS 421 / Summer 2024
geometry: "left=2cm,right=2cm,top=2cm,bottom=2cm"
---

Introduction
------------

##### Where we've been

In years past there has seemingly been a chasm between fully imperative languages and purely functional
languages.  While functional programming languages maintained their significance in academia their use in practical
application was often somewhat niche.  For example, while Erlang has been around since 1986, it's use outside of telecom
was few and far between. Procedural imperative languages such as Fortran and C became the dominant player for some time,
until eventually ceding much market share to object-oriented languages such as C++ and Java.  

##### Where we are

Imperative languages can at times have difficulty navigating the concurrent and asynchronous
nature of the modern systems.  Often much care must be taken when writing concurrent (especially parallel) 
applications.  Just as memory management can be the bane of languages like C, concurrency primitives such as locks,
mutexes and semaphores, can lead to the same level of frustrations for software developers.  Further is the issue of
asynchronous development, which procedural code by nature does not mesh well with.  In the past this was dealt with
using polling or callback based APIs.  Recently, languages such as Rust, JavaScript and Python have added the
async/await concept, to make non-blocking asynchronous code appear to function in a blocking manner, but this is
typically just syntactic sugar on top of existing polling or callback based APIs.

##### Where (I think) we're headed

Given that shifts in the interconnectivity, hardware and general interfacing patterns in the past have driven how we 
develop software, it is only logical to think the same would arise today.  While functional languages have
recently seen a large increase in industry, there has also been a drive to combine declarative programming paradigms
(both functional and reactive) with existing imperative programming languages.  Furthermore, you are seeing languages
designed in this way from the very beginning with a mixed or fluent paradigm.  Typically, these elements
include higher order functions, lambda expressions and functors.  More recently, either natively or via third party
libraries you are increasingly seeing these languages introduce pattern matching, algebraic data types and monad or
monad like containers.  The remainder of the paper will use one such fluent language, Scala, as well as supporting open
source libraries to show the effectiveness of mixing imperative and functional concepts to build software that is
performant, easier to reason about and easier to test.

Scala Fundamentals
------------------

To demonstrate the combination of imperative and functional programming concepts, this paper will use the Scala
programming language.  While a native compiler on top of LLVM as well as a javascript transpiler exist for Scala, it's
first and still most prevalent compiler generates Java bytecode to be run on a Java virtual machine.   The paper will
also explore multiple open source libraries that are part of the Scala ecosystem.  For the imperative parts of Scala,
it very much resembles Java in its syntax and object-oriented semantics. 

When taking into account the functional aspects of Scala, you will see that it offers much of what you would expect
from such a language - higher order functions, currying and immutability.  Facilities you'd come to expect out of
a language like Haskell such as algebraic data types and pattern matching are also present.  With scala being both
functional and object-oriented, polymorphism is in turn supported both via type classes as well inheritance. 
Both are both supported by what is a `trait` in Scala.  Syntactically inheritance and type classes are defined the same,
it is the context in which a trait is used which determines which is being expressed.   

Scala does not have built-in type classes to support functor or a monad, however there are intrinsic language
characteristics for supporting them (there are libraries that add explicit type classes for them, but those are out
of scope for this paper).   Functors are recognized by the existence of a map member function of type 
`F[A] => (A => B) => F[B]` similar to the signature of `fmap` in the Haskell Functor type class.  The arguments are
reversed compared to Haskell, as a consequence of how we must evaluate in Scala, where the map function is
a member of an object, and thus the function is evaluated as `obj.map(...)`.

Monad's in Scala are implemented by the existence of a single argument constructor to act as the unit 
(`return` in Haskell) definition and a `flatMap` function to act as the bind definition.  Similar to the map
function, flatMap carries the signature `M[A] => (A => M[B]) => M[B]`.   To demonstrate what a simple trait may look
like enforcing flatMap, we present the following (omitting the unit definition here as this is implemented via
a special constructor like syntactic sugar):


```scala
trait Monad[A] {
  def flatMap[B](f: A => Monad[B]): Monad[B]
}
```


The above is enforcing via the inheritance usage of a trait.  If we wanted to define a type class using a
trait we would have defined it something like:


```scala
trait Monad[M[A]] {
  def flatMap[B](m: M[A], f: A => M[B]): M[B]
}
```


While explicit type classes do not exist for functor or monad, implementing the aforementioned map and flatMap provide
the ability to use the functor or monad in a for comprehension.  When sequencing a single functor, of the form:


```scala
for (element <- functor) yield ???
```


the map function will be used, and the above can be thought more of as a list comprehension in Haskell.  However, when
sequencing over multiple monads, taking on the form:


```scala
for {
  element1 <- monad1
  element2 <- monad2
} yield ???
```


the flatMap function will be used, and the above now functions more like the `do` notation of Haskell.  It is worth 
noting, functor only objects do not support multi-sequencing due to the lack of a flatMap function.   Scala has many
monads out of the box, including a few of the most popular ones of Haskell - List, Either and Maybe (called Option in
Scala).  The main concern of this paper is to cover material in the context of concurrent programming, so let's review 
two important monads scala provides to help facilitate this.

Try Monad
---------

While the main concern is to focus on concurrent development, we would be remiss not to discuss error handling.  Scala
contains a monad named `Try` that allows us to wrap arbitrarily large expressions.  
The resultant value of the evaluation of the expression is lifted into a
`Success` which implements the Try trait.  In Haskell parlance we can think of this as one of Try's type constructors.
If the evaluation throws an exception, then the Throwable is caught and lifted into an instance of `Failure`.
This provides us with a convenient means to wrap and compose impure side effecting code.
The following is a demonstration of Try:


* _Lift execution into a Try.  As we map over the Try, we are passed the values of
successful computation.  If at any point a Throwable is thrown, the execution is
short-circuited, and the Throwable is now lifted into the Try as a Failure_
```scala
val simple: Try[String] = {
  val longComputation = Try((1L until 100000L).foldRight(0L)((a, b) => a + b ))
  longComputation
    .map(_ - 4999949958L)
    .map(answer => s"The meaning of life is $answer")
}
```


* _Using a for-comprehension to extract values to construct further Trys_
```scala
val composed: Try[Int] = {
  val wrapped1: Try[Int] = Try(MockApi.blockingCall)
  val wrapped2: Try[Int] = Try(MockApi.blockingCall)
  for {
    inputA <- wrapped1
    inputB <- wrapped2
    sum  <- Try(inputA + inputB)
  } yield sum
}
```


* _Success or failure can be gleaned and the successful value or Throwable can
be extracted using pattern matching_
```scala
val patternMatching: String =
  simple match {
    case Success(result) => result
    case Failure(exception) => exception.getMessage
  }
```


Future Monad
------------

While the Try monad gave us a way to compose simple imperative and/or side-effecting code, the execution still happens
synchronously within the same execution context of the Try.  That is to say it happens directly in the thread you
created the Try in and the constructor of Try blocks while the code runs.  As with Try, the Future monad's unit 
(constructor) parameter is defined as call-by-name.  This allows for passing in any arbitrarily large expression,
but also due to the lazy nature of call-by-name allows us to transfer the evaluation of that expression to a different
execution context, and this is precisely what Future does.  One nuance of the constructor definition for Future is that
it is meant to be curried.  

Let us step back and clarify constructors for monads in Scala.  While they have been described
as being single argument constructors, these are not normal constructors in the sense of direct instantiation of the
object.  Scala allows defining a companion `object` alongside of a `class` definition.  To skip over some of the gory 
details, these companion objects are often used to define static members of a class.  One special
function that can be defined on companion objects is the `apply` method.  This provides syntactic sugar that allows
you to specify only the object/class name followed by parens, which will then invoke the apply method matching the
argument signature you have passed if one exists. For example Future defines a companion object with an apply as
follows:


```scala
object Future {
  final def apply[T](body: => T)(implicit executor: ExecutionContext): Future[T] =
    unit.map(_ => body)
}
```


This may leave you wondering about the second curried argument `executor` in the apply method.  This requires one
further clarification.  As can be seen, the parameter is annotated with the `implicit` qualifier.  In Scala, an implicit
parameter allows you to define an implicit value or function of that data type within certain designated scopes
(I will not cover the scoping semantics here as it can be quite complex).  At runtime this value or function
invocation are fed to the implicit argument.  Thus, you will typically have an implicit `ExecutionContext` in scope
to feed to your Future:


```scala
// Scala would automatically pass this to your curried Future constructor at runtime.
implicit val executionContext: ExecutionContext = ???

//arbitrary expression provided to future 
val future = Future { ??? }
```


ExecutionContext is a trait which defines an execute method tha takes a java.lang.Runnable as it's only argument.
Future will wrap the expression you passed into it's constructor in a Runnable and then leverage the ExecutionContext to
execute your code.   This provides the benefit that you can control exactly how the Future executes your code
concurrently.  Your ExecutionContext can be backed by a fixed thread, a thread pool or just run inline on the thread
you are currently executing in.  Recall that Scala is not restricted to only the jvm.  The implementation that 
transpiles to javascript is then able to use an ExecutionContext that instead can make use of a javascript Promise  
or some other abstraction to provide concurrency.
 Similar to how Try allows us to compose synchronous code, and encapsulate failure, Future allows us to do the same
for concurrent tasks.  The composed Futures need not share the same ExecutionContext, and can thus be managed in
different ways.  Whichever way that may be, we are now able to reason about the concurrent execution in a more natural
way.  When composing futures via constructs like for comprehensions it begins to look very much much like imperative
code.  The following is a demonstration of Future:


* _Lift execution into a Future.  As we flatMap over the Future, we are passed the values of
successful computation.  If at any point a Throwable is thrown, the execution is
short-circuited, and the Throwable is now lifted into the Future.  It is worth noting that
the execution of the closures inside the flatMap and the Futures that are generated within
will each be executed in a different context._


```scala
val bind: Future[String] = {
  val longComputation = Future((1L until 100000L).foldRight(0L)((a, b) => a + b ))
  longComputation
    .flatMap(n => Future(n - 4999949958L))
    .flatMap(answer => Future(s"The meaning of life is $answer"))
}
```


* _Using a for-comprehension to extract values to construct further Futures
as with the above, the bits happening inside the for comprehension and in the yield itself
are executed in a separate context from the Futures themselves_
```scala
val composed: Future[Int] = {
  val wrappedLongExec: Future[Int] = Future(MockApi.blockingCall)
  val nonBlocking: Future[Int] = MockApi.nonBlockingCall
  for {
    inputA <- wrappedLongExec
    inputB <- nonBlocking
    sum  <- MockApi.nonBlockingCallWithInput(inputA, inputB)
  } yield sum
}
```


* _Lift a value directly into a Future, without invoking any concurrent execution.
Useful when you want to avoid context switching and/or already know the value to return._
```scala
val simpleLift: Future[String] = {
  Future
    .successful(42)
    .map(answer => s"The meaning of life is $answer")
}
```


* _Use a Promise to lift a callback's execution into a Future.  A Promise contains a success
method that we can call when we have a value to complete it with.  This
then completes an internal Future on the Promise that can be accessed
via a future member property.  This allows us to pass a closure to a callback API that invokes
the success method. Externally the user interfaces with and collects the result via
the promises future member._
```scala
def callbackLift: Future[Int] = {
  val promise = Promise[Int]()
  MockApi.nonBlockingCallWithCallback(promise.success)
  promise.future
}
```


Effectful Programming
---------------------

While Try and Future are powerful monads for composing imperative code in a more functional manner, they are both 
wrappers around an evaluation.  Although their unit parameters are call-by-name they are not truly lazy in  their
evaluation of the expression that they wrap.  Try will execute immediately and Future will execute as soon as it's 
ExecutionContext will allow it. They are themselves by nature side-effecting and can only be evaluated once.  What we
really want to do is wrap the potential of an expression rather than its evaluation.  This is where effects enter
the picture, as they provide us a container for the description of an action as opposed to the result of an action.
 Scala does not have an effect abstraction built-in, however the Scala ecosystem contains multiple libraries which 
provide effect monads.  Arguably the most popular of these, and the one that will be used as an example is Cats Effect.
The core entity in Cats Effect is the IO monad.  The IO monad functions in the same way as its identically named
counterpart from Haskell.  IO allows you to wrap pure values as well as side-effecting actions, and then compose 
arbitrary IO providing the illusion of complete functional purity.

IO has several methods for lifting pure values or actions into it.  In addition to pure values or arbitrary expressions
there are conveniences to lift Scala Future, Try, Either and Option monads directly into an IO, traverse sequence types 
or lift asynchronous/callback based actions.  Facilities are also provided evaluate IOs in parallel and
while the purpose of this paper is to focus on programming paradigms, the way Cats Effects manages IO execution and
concurrency is worth mentioning.  Under the hood it uses the concept of fibers (also referred to as green threads)
that are lightweight structures on top of native and/or virtual machine threads.  The Cats Effect runtime manages
execution via these fibers on top of normal threads, reducing OS/VM context switching and providing ways to 
cancel/interrupt execution not typically possible in something like a typical JVM thread.  Programs are then a
composition of IOs which are feed through the Cat Effect managed runtime. 
Let's take a look at a couple examples of creating IO effect, and a sample program:


* _Simulate long-running synchronous API call.  The >> operator in analogous to
a bind operation which doesn't actually pass the contained result of the
first IO to the second._
```scala
def simple: IO[String] =
  IO.sleep(1.second) >> IO.pure(42).map(answer => s"The meaning of life is $answer")
```


* _Lift an async callback based API into an IO_
```scala
def asyncCallback: IO[Int] =
  IO.async_(cb => MockApi.nonBlockingCallWithCallback(num => cb(Right(num))))
```


* _Execute multiple IO in parallel_
```scala
def parallel: IO[Seq[Int]] =
  IO.parSequenceN(2)(Seq(simple, asyncCallback))
```


* _Small example of a program written using Cats Effect.  This program reads 
fully qualified domain names from a text file, then resolves the IP address
of those domain names through DNS._
```scala
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}

object IOSampleApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      fqdns <- IOExamples.readFqdnList
      fqdnsWithDummy <- IO("dummy.bad" :: fqdns)
      _ <- IO.parTraverseN(fqdnsWithDummy.length)(fqdnsWithDummy)(hostname =>
        IOExamples
          .resolveIP(hostname)
          .map(address => s"Successfully resolved host hostname with ip $address")
          .handleError(error => s"Unable to resolve host ${error.getMessage}")
          .flatMap(Console[IO].println)
      )
    } yield ExitCode.Success
  }
}
```


In the example program we are extending the IOApp trait, which provides an abstract run function.  Internally this trait
contains the runtime dependent entry point setup (i.e. `static void main` on the JVM), which will in turn execute your
run function definition.  One simply need return an `IO[ExitCode]`, so the general idea is to compose several IO
which in the end through various functor, monadic and similar transformations end with an ExitCode as the contained
value.  The referenced function IOExamples.readFqdnList has return type `IO[List[String]]` and returns a list of
fully qualified domain names read from some arbitrary location.  IOExamples.resolveIP has return type `IO[String]`
and which represents a DNS resolved IP address.  The IO.parTraverseN allows us to traverse over a sequence, here
being a sequence FQDNs (with the first parameter controlling the number of fibers to execute on concurrently).
The last parameter of parTraverseN expects a HOF which takes the sequence's contained type as a parameter and
returns an IO to be evaluated in parallel.  We further transform the IO from IOExamples.resolveIP
using map (functor style) and flatMap (monad style).  

This is all composed within a for comprehension which ultimately binds to ExitCode.  This is a very small
example for demonstration, but you can imagine for larger programs you simply compose smaller compositions into 
increasingly larger ones.  This allows you to construct parallel and asynchronous code in a way that looks very similar
to imperative code, while in the end being much easier to reason about.  They also allow us to easily decompose our 
application down to its atomic structure if you will (if one can imagine effects as being the atoms).  These discrete
bits are then much easier to unit test and verify, especially when encapsulating concurrent actions.
They are also easier to integration test as you can compose them with mock IOs and/or into mini programs which simulate 
interactions of a real world program.


Reactive Programming
--------------------

The final subject to touch on is a newer but very closely related programming style to utilizing effects called
reactive programming.  The concept itself unlike effects, is not directly tied to functional programming.  Reactive
programming itself is a declarative paradigm which is meant as a way to deal with streams of data.  Yet often times, the
syntax and semantics will employ constructs of both imperative and functional programming.  Programs are often modelled
as pipelines or graphs, through which data is transformed.  These transformations provide behavior seen in that
of functors and monads, or common functional patterns performed on sequences such as fold aggregations.  In most
languages reactive programming comes in the form of a library.  The Scala ecosystem has various implementations,
some which take an actor model approach (which is out of scope for this discussion), but most which lean almost
exclusively in the functional camp.  

One such library is FS2, which itself is built on top of Cats Effect.  Being based on Cats
Effect, it makes use of the IO monad quite heavily.  FS2 itself is built around Stream, a higher kinded type
that takes two type parameters.  The first type parameter is to provide a monad type constructor, and the second is to
provide an output type.  While you can provide your own monad evaluator to FS2, out of the box it is designed to work
with the IO monad.   A Stream has several methods on it that allow you to transform elements as they pass
through what can be thought of as a directed graph where each node represents some transformation.  Some of these
transforms such as map convert from one output type to another.  Others operate more closely to a bind, and expect a
function that takes a prior output type and returns the monad type of the Stream.  Streams are seeded
from a source of data that can be viewed as a (possibly infinite) generator of discrete values, such as a sequence type 
or a socket connection. The following are examples of creating a Stream, and a sample program:


* _Simple stream created from an apply (which takes a varargs set of elements).
The stream adds 100 to each element_
```scala
def simpleStream: Stream[IO, Int] = 
  Stream(1, 2, 3, 4, 5).map(100.+)
```


* _Create a stream that just continuously evaluates an IO.  Here the IO
is lifted from an API call that returns a Future.   It filters the data
for even numbers and takes the first 5 it sees._
```scala
def fromFutureWithFilter: Stream[IO, Int] =
  Stream
    .repeatEval(IO.fromFuture(IO(MockApi.nonBlockingCall)))
    .filter(n => (n % 2) == 0)
    .take(5)
```


* _Build a stream using Queue.  In this example, we use this queue
in a closure that is passed to a callback based API.
An internal stream is created from the evaluation of an IO,
and thus we flatten out the surrounding stream to lift the internal one out._
```scala
def queueStream: Stream[IO, Int] =
  Stream.eval {
    for {
      queue <- Queue.unbounded[IO, Option[Int]]
      events <- IO.fromFuture(IO(MockApi.nonBlockingCallWithMultiCallback[IO[Unit]](n => queue.offer(Some(n)), 5)))
      _ <- IO.parSequenceN(5)(events.toList)
      _ <- queue.offer(None)
    } yield Stream.fromQueueNoneTerminated(queue).map(n => n * 2 + 1000)
  }.flatten
```


* _Small example of a program written using FS2.  This program reads
fully qualified domain names from a text file, then resolves the IP address
of those domain names through DNS._
```scala
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream

import java.net.InetAddress

object FS2SampleApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Stream
      .eval(IOExamples.readFqdnList)
      .flatMap(Stream.emits)
      .append(Stream("dummy.dummy"))
      .parEvalMapUnordered(4) { fqdn =>
        IO(InetAddress.getByName(fqdn))
          .map(Right.apply[Throwable, InetAddress])
          .handleError(Left.apply[Throwable, InetAddress])
      }
      .parEvalMapUnordered(4) {
        case Right(host) =>
          Console[IO].println(s"Successfully resolved host ${host.getHostName} with ip ${host.getHostAddress}")
        case Left(error) =>
          Console[IO].println(s"Unable to resolve host ${error.getMessage}")
      }
      .compile
      .drain
      .as(ExitCode.Success)
}
```


The example program achieves the same result as the Cats Effect sample we showed earlier, however using the Stream 
abstraction. We first create a stream from IOExamples.readFqdnList which as before returns an `IO[List[String]]`.  We
then use flatMap to perform a bind like operation over another Stream, which is created using Stream.emits.  
Stream.emits simply creates a Stream from a sequence type.  Next we append another Stream created from a single element.
These parts compose the elements we will send downstream to other operators for transformation.

Moving to the first parEvalMapUnordered this process upstream elements in parallel.  It takes a closure which creates
an IO monad for evaluation.  In this step which try to resolve the IP address.  Note that we are mapping the result
of this in an Either.  The second parEvalMapUnordered,  pattern matches on the Either emitted from the prior transform
evaluating an IO which merely prints out success or not.

The compile function generates a projection that allows the Stream to be converted to it's monad type parameter
in a number of ways.  In this instance we use the drain method which merely runs the Stream to completion and returns
`M[Unit]` where M is our monad type constructor (IO in the example).  There are other rich methods on the projection
such as asList which will return the results as `M[List[O]]` where M is the monad type of the stream, and O is the type
of the output of the final transformation.   There are similar methods that perform operations such as a count on
the elements or performing a right fold over the final elements.


Conclusion
----------

We have seen by example how one can combine imperative and functional program to compose heavily concurrent and
side-effecting code in a more discernible manner.  Scala was a logical choice for demonstration as the language
being built to facilitate both paradigms.   Encouragingly, you can see many traditional imperative languages adding
functional aspects as time goes on.  Newer languages like Rust and Dart, like Scala, are being designed from the start
to be multi-paradigm.   In addition, it seems almost every language has one or more popular libraries to provide
reactive extensions.   As systems grow in number of cores, be it CPU, GPU or TPU, I feel that language constructs
and libraries such as these will increase in popularity.   To take it one step further, one can envision these 
constructs will further support distributed systems with libraries increasingly taking the same approach to providing 
location transparency and distributed concurrency.


Appendix
--------

The accompanying repository for this document is located at
[https://github.com/moonkev/cs421-project](https://github.com/moonkev/cs421-project)

Examples can be found @ src/main/scala/com/github/moonkev/*Examples.scala

Samples apps can be found @ src/main/scala/com/github/moonkev/*SampleApp.scala

Tests cases can be found @ src/test/scala/com/github/moonkev/*Spec.scala

To run the test cases, run the following command (replace `./gradlew` with `gradlew.bat` on Windows)
```shell
$ ./gradlew test 
```

To run the sample Cats Effect app
```shell
$ ./gradlew runcats 
```

To run the sample FS2 app
```shell
$ ./gradlew runfs2 
```


References
----------

[Alexander17] Alvin Alexander _Functional Programming, Simplified (Scala Edition)_, CreateSpace Independent Publishing Platform

[Gifford86] David K. Gifford and John M Lucassen _Integrating Functional and Imperative Programming_, MIT Laboratory for Computer Science (1986)

[Lipovaca11] Miran Lipovaca _Learn you a Haskell for Great Good!_, No Starch Press (2011)

[Cats24] Typelevel (Various) _Cats Effect_, [https://typelevel.org/cats-effect/](https://typelevel.org/cats-effect/) (2017-2022)

[FS24] Typelevel (Various) _FS2_, [https://fs2.io/#/](https://fs2.io/#/) (2024)
