package jbi

import Console._
import java.io.File
import sys.process._
import collection._

object JBI {
  def run(cmd: String) {
    err.println(cmd)
    val result = cmd ! ;
    if(result != 0) {
      err.println("ERROR: Command failed")
      exit(1)
    }
  }

  def findFiles(f: File, fileFilter: File => Boolean): List[File] = {
    val files = f.listFiles
    val these = if(files == null) List() else files.toList
    these.filter(fileFilter) ++ these.filter(_.isDirectory).flatMap(findFiles(_, fileFilter))
  }

  def getClasses(javaFile: File): Seq[File] = {
    val str = javaFile.getAbsolutePath
    List(new File(str.replace(".java", ".class").replace("src", "classes")))
  }

  def javac(srcDir: String, tgtDir: String, libs: List[String] = List.empty, args: List[String] = List.empty) {
    val srcFiles = findFiles(new File(srcDir), (f: File) => f.getName.endsWith(".java"))
    new File(tgtDir).mkdirs
    err.println("Found %d Java source files".format(srcFiles.size))

    val modified = srcFiles.filter(srcFile => getClasses(srcFile).exists(classFile => srcFile.lastModified > classFile.lastModified || classFile.length == 0))
    err.println("%d files need recompilation".format(modified.size))
    
    if(modified.size > 0) {
      val cp = "-cp %s".format( (libs ++ List(tgtDir)).mkString(":") )
      run("javac -d %s %s %s %s".format(tgtDir, args.mkString(" "), cp, modified.mkString(" ")))
    }
  }

  def parseScalaDeps(depFile: String): (Map[String, Set[File]], Map[String, Set[File]]) = {

    val deps = new mutable.HashMap[String,mutable.HashSet[String]]
    val classes = new mutable.HashMap[String,mutable.HashSet[String]]

    var sec = 0
    for(line <- io.Source.fromFile(depFile).getLines) {
      if(line == "-------") {
        sec += 1
      } else if(sec == 1) {
        val Array(what, affects) = line.split(" -> ")
        // remove "../"
	val set = deps.getOrElseUpdate(what.substring(3), new mutable.HashSet[String])
	set += affects.substring(3)
      } else if(sec == 2) {
        val Array(srcFile, classFile) = line.split(" -> ")
	val srcFile1 = srcFile.substring(3)
	val set = classes.getOrElseUpdate(srcFile1, new mutable.HashSet[String])
	set += "bin/"+classFile // XXX: bin
      }
    }
    (deps.mapValues(_.map(new File(_))), classes.mapValues(_.map(new File(_))))
  }

  def scalac(srcDir: String, tgtDir: String, libs: List[String] = List.empty, args: List[String] = List.empty) {
    val srcFiles: Seq[File] = findFiles(new File(srcDir), (f: File) => f.getName.endsWith(".scala"))
    err.println("Found %d Scala source files".format(srcFiles.size))

    val depFile = "%s/.scaladeps".format(tgtDir)
    val recompile = if(new File(depFile).exists) {
        val (deps: Map[String, Set[File]], classes: Map[String, Set[File]]) = parseScalaDeps(depFile)

	val modified = srcFiles.filter(srcFile => classes.getOrElse(srcFile.toString, Set.empty).exists(classFile => srcFile.lastModified > classFile.lastModified || classFile.length == 0))
	err.println("%d source files modified".format(modified.size))

        val affected = modified.flatMap(srcFile => deps.getOrElse(srcFile.toString, Set.empty))
        err.println("%d source files need recompilation".format(affected.size))

	affected
    } else {
        err.println("No dependency data found. Recompiling everything")
        srcFiles
    }
    
    if(recompile.size > 0) {
      val cp = "-cp %s".format( (libs ++ List(tgtDir)).mkString(":") )
      new File(tgtDir).mkdirs
      run("scalac -make:transitive -dependencyfile %s -d %s %s %s %s".format(depFile, tgtDir, args.mkString(" "), cp, recompile.mkString(" ")))
    }
  }
}