package com.aryanpaode.portfolioforgeai

import android.database.sqlite.SQLiteDatabase
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aryanpaode.portfolioforgeai.data.db.*
import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot
import com.aryanpaode.portfolioforgeai.service.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Requires an Android emulator/device. Not executed by tools/test_core.py. */
@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {
 private val context = InstrumentationRegistry.getInstrumentation().targetContext

 @Test fun roomValidatesV1ToV2AndRetainsRows() = runBlocking {
  val name="instrumented-migration-${UUID.randomUUID()}.db"
  val file=context.getDatabasePath(name); file.parentFile?.mkdirs()
  val old=SQLiteDatabase.openOrCreateDatabase(file,null)
  val seed=InstrumentationRegistry.getInstrumentation().context.assets.open("v1_schema.sql").bufferedReader().use {it.readText()}
  seed.lines().filterNot {it.trimStart().startsWith("--")}.joinToString("\n").split(';').map {it.trim()}.filter {it.isNotEmpty()}.forEach {old.execSQL(it)}
  old.close()
  val db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(DatabaseMigrations.MIGRATION_1_2).build()
  try {
   assertEquals("Test Student",db.profileDao().get()?.name)
   assertEquals("",db.profileDao().get()?.portfolioUrl)
   assertEquals("Original verified text",db.documentDao().getAll().single().verifiedText)
   assertEquals("Original research topic",db.careerItemDao().getAll().single().title)
   assertEquals("",db.careerItemDao().getAll().single().sourceDocumentId)
   assertEquals("Python",db.skillDao().getAll().single().name)
  } finally {db.close();context.deleteDatabase(name)}
 }
 @Test fun differentAccountIdsHaveSeparateRoomFiles() = runBlocking {
  val prefix="instrumented-${UUID.randomUUID()}"; val a="$prefix-A"; val b="$prefix-B"
  val factory=WorkspaceDatabases(context);val da=factory.get(a);val db=factory.get(b)
  try {
   da.profileDao().upsert(ProfileEntity(name="Account A"))
   assertNull(db.profileDao().get())
   db.profileDao().upsert(ProfileEntity(name="Account B"))
   assertEquals("Account A",da.profileDao().get()?.name)
   assertEquals("Account B",db.profileDao().get()?.name)
  } finally {
   da.close();db.close()
   context.deleteDatabase("portfolioforge_${WorkspaceDatabases.folder(a)}.db")
   context.deleteDatabase("portfolioforge_${WorkspaceDatabases.folder(b)}.db")
  }
 }
 @Test fun generatedPdfContainsIndependentSectionsAndRealLinks() {
  val workspace="instrumented-pdf-${UUID.randomUUID()}"
  val snapshot=Snapshot(ProfileEntity(name="Test Student"),emptyList(),listOf(
   CareerItemEntity("c","Conference","ICSCES","Example Institute","Participant","September 2026"),
   CareerItemEntity("p","Publication","Publication Topic","Example Press","Verified description. ".repeat(250),"2026","https://example.org/paper")),emptyList(),workspace)
  try {
   val file=ResumePdfGenerator(context).generate(snapshot)
   PDFBoxResourceLoader.init(context)
   PDDocument.load(file).use { doc ->
    val text=PDFTextStripper().getText(doc)
    assertTrue(text.contains("ICSCES"));assertTrue(text.contains("Publication Topic"));assertTrue(text.contains("Example Press"))
    assertTrue(doc.numberOfPages>1)
    assertTrue((0 until doc.numberOfPages).any {n ->doc.getPage(n).annotations.any {it is PDAnnotationLink}})
   }
  } finally {File(context.filesDir,"exports/${WorkspaceDatabases.folder(workspace)}").deleteRecursively()}
 }
 @Test fun pdfTextAfterSixthPageIsNotSilentlySkipped() = runBlocking {
  val file=File(context.cacheDir,"instrumented-seven-pages-${UUID.randomUUID()}.pdf")
  val pdf=PdfDocument()
  for(n in 1..7) {
   val page=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,n).create())
   val paint=Paint().apply {textSize=14f}
   page.canvas.drawText("Synthetic document page $n for PortfolioForge extraction testing.",25f,80f,paint)
   page.canvas.drawText(if(n==7) "PAGE_SEVEN_MARKER Python and LangChain" else "Readable text layer with enough characters for extraction.",25f,110f,paint)
   pdf.finishPage(page)
  }
  try {
   file.outputStream().use {pdf.writeTo(it)};pdf.close()
   val result=OcrService(context).extract(file,"application/pdf")
   assertEquals(7,result.pagesRead);assertTrue(result.text.contains("PAGE_SEVEN_MARKER"))
  } finally {file.delete()}
 }
 @Test fun fileProviderRefusesRawPrivateDocuments() {
  var blocked=false
  try {FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",File(context.filesDir,"documents/not-shared.pdf"))}
  catch(e:IllegalArgumentException) {blocked=true}
  assertTrue(blocked)
 }
}
