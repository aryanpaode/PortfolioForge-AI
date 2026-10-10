#!/usr/bin/env python3
"""Compile/run the real pure-Kotlin sources without an Android SDK.
Uses temporary Room annotation stand-ins and tiny JUnit assertion/annotation
stand-ins ONLY for these JVM tests; these are never packaged in the Android app.
Requires Python 3, a JDK and kotlinc >= 1.8. For actual Gradle tests use
    gradlew.bat testDebugUnitTest
This runner is not an Android build, an OCR test, or a Firebase/device test.
"""
from pathlib import Path
import os, shutil, subprocess, tempfile, sys
ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/aryanpaode/portfolioforgeai'
compiler=shutil.which('kotlinc') or shutil.which('kotlinc.bat')
if not compiler: raise SystemExit('kotlinc not found. Run the JUnit tests with gradlew.bat testDebugUnitTest instead.')
for cmd in ['java','javac']:
    if not shutil.which(cmd): raise SystemExit(f'{cmd} is missing. Install/select a JDK first.')
services=['DocumentClassifier','DocumentExtractionService','SkillIntelligence','StructuredImportService','UrlPolicy','EntryIdentity','PortfolioRenderer']
with tempfile.TemporaryDirectory(prefix='portfolioforge_core_') as tmp:
    base=Path(tmp); (base/'org/junit').mkdir(parents=True)
    (base/'org/junit/Test.java').write_text('package org.junit; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface Test {}')
    (base/'org/junit/Assert.java').write_text('''package org.junit;
public final class Assert {
 public static void assertTrue(boolean x) { if(!x) throw new AssertionError("Expected true"); }
 public static void assertFalse(boolean x) { if(x) throw new AssertionError("Expected false"); }
 public static void assertNull(Object x) { if(x!=null) throw new AssertionError("Expected null but was: "+x); }
 public static void assertEquals(Object a,Object b) { if(!java.util.Objects.equals(a,b)) throw new AssertionError("Expected ["+a+"] but was ["+b+"]"); }
}''')
    subprocess.run(['javac','--release','17','-d',str(base),*map(str,(base/'org/junit').glob('*.java'))],check=True)
    (base/'RoomAnnotations.kt').write_text('''package androidx.room
annotation class Entity(val tableName: String = "", val indices: Array<Index> = [])
annotation class Index(val value: Array<String>, val unique: Boolean = false)
annotation class PrimaryKey
annotation class ColumnInfo(val defaultValue: String = "")
''')
    (base/'Runner.kt').write_text('''import com.aryanpaode.portfolioforgeai.CoreRegressionTest
fun main() {
 val test = CoreRegressionTest(); var passed=0; var failed=0
 val methods=CoreRegressionTest::class.java.declaredMethods.filter { it.isAnnotationPresent(org.junit.Test::class.java) }.sortedBy {it.name}
 for(method in methods) {
  try {method.invoke(test); println("PASS "+method.name); passed++}
  catch(e: java.lang.reflect.InvocationTargetException) {println("FAIL "+method.name+": "+e.targetException); failed++}
 }
 println("RESULT: $passed passed, $failed failed (${'$'}{methods.size} pure JVM tests)")
 if(failed>0) throw IllegalStateException("Core regressions failed")
}
'''.replace("${'$'}{methods.size}","${methods.size}"))
    files=list((PKG/'data/model').glob('*.kt'))+[PKG/'data/repository/Snapshot.kt']+[PKG/f'service/{s}.kt' for s in services]
    tests=ROOT/'app/src/test/java/com/aryanpaode/portfolioforgeai/CoreRegressionTest.kt'
    jar=base/'tests.jar'
    subprocess.run([compiler,'-language-version','1.8','-api-version','1.8','-jvm-target','17','-classpath',str(base),*map(str,files),str(tests),str(base/'RoomAnnotations.kt'),str(base/'Runner.kt'),'-include-runtime','-d',str(jar)],check=True)
    result=subprocess.run(['java','-cp',str(jar)+os.pathsep+str(base),'RunnerKt'])
    sys.exit(result.returncode)
