Just Build It

Enough with the complicated build systems already.

=Compiling JBI=

```bash
scalac -d bin jbi.scala
```

=Compiling Using JBI=

Make a Scala file called BuildIt:

```scala
import jbi._
JBI.scalac("src", "bin")
```

```bash
jbi
```
