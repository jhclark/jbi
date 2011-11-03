Just Build It
=============

Enough with the complicated build systems already.

Compiling JBI
-------------

```bash
scalac -d bin jbi.scala
```

Compiling Scala Using JBI
-------------------------

Make a Scala file called BuildIt:

```scala
import jbi._
JBI.scalac("src", "bin")
JBI.jar("bin", "example.jar", mainClass="A")
```

Then run:

```bash
jbi
```

See the example/ directory.

Compiling Java Using JBI
------------------------

Just like above. Use javac instead of scalac.
