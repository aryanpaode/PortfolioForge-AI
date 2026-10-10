package com.aryanpaode.portfolioforgeai

import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot
import com.aryanpaode.portfolioforgeai.service.*
import org.junit.Assert.*
import org.junit.Test

/** Synthetic transcribed evidence; not a test of the user's real certificates or OCR accuracy. */
class CoreRegressionTest {
    private val intelligence = SkillIntelligence()
    private val classifier = DocumentClassifier()
    private val extractor = DocumentExtractionService(intelligence, classifier, StructuredImportService(intelligence))
    private fun parse(text: String, category: String = "Auto detect", filename: String = "sample.pdf") = extractor.parse("doc-1", text.trimIndent(), filename, category)
    private fun names(text: String) = intelligence.extract(text, "test").map { it.name }.toSet()
    private fun portfolio(items: List<CareerItemEntity>, skills: List<SkillEntity> = emptyList()) = PortfolioRenderer.render(Snapshot(ProfileEntity(name = "Test Student"), emptyList(), items, skills))
    private val course = """
        Coursera
        Test Student
        has successfully completed
        Retrieval-Augmented Generation (RAG)
        an online course authorized by IBM and offered through Coursera
        Completion date: October 3, 2026
        https://www.coursera.org/account/accomplishments/verify/EXAMPLE
    """.trimIndent()
    private val conference = """
        Certificate of Participation
        This is to certify that Test Student participated in
        Conference name: ICSCES
        Event date: 15 September 2026
        Organizer: Example Institute
    """.trimIndent()
    @Test fun courseCreatesCertification() { assertEquals("Certification", parse(course).items.single().type) }
    @Test fun courseTitlePreserved() { assertEquals("Retrieval-Augmented Generation (RAG)", parse(course).items.single().title) }
    @Test fun platformAndIssuerPreserved() { assertEquals("IBM / Coursera", parse(course).items.single().subtitle) }
    @Test fun courseMonthYear() { assertEquals("October 2026", parse(course).items.single().date) }
    @Test fun courseVerificationLink() { assertTrue(parse(course).items.single().link.contains("/verify/EXAMPLE")) }
    @Test fun ragAliasExtracted() { assertTrue(parse(course).skills.any { it.name == "Retrieval-Augmented Generation (RAG)" }) }
    @Test fun noInventedPythonOrPaddleOcr() { val found=parse(course).skills.map {it.name}; assertFalse("Python" in found); assertFalse("PaddleOCR" in found) }
    @Test fun filenameNeverAwardsSkills() { assertTrue(parse("Certificate\nCourse: Philosophy\nIssuer: Example", filename="Python_PaddleOCR_RAG.pdf").skills.isEmpty()) }
    @Test fun sourceParameterNeverAwardsSkills() { assertTrue(intelligence.extract("A certificate of attendance", "PaddleOCR Python Java").isEmpty()) }
    @Test fun conferenceCreatesEntryWithZeroSkills() { val r=parse(conference); assertEquals("Conference",r.category); assertEquals(1,r.items.size); assertTrue(r.skills.isEmpty()) }
    @Test fun conferenceAcronymNotExpanded() { assertEquals("ICSCES",parse(conference).items.single().title) }
    @Test fun conferenceMonthYearAndParticipation() { val i=parse(conference).items.single();assertEquals("September 2026",i.date);assertEquals("Participant",i.description) }
    @Test fun conferenceTopicsRequireUserConfirmation() { assertTrue(parse(conference+"\nTopics: Machine Learning").skillsAreTopics) }
    @Test fun internationalNotIntern() { assertEquals("Conference",classifier.classify("Participation in the International Conference on Computing")) }
    @Test fun internationalCourseNotIntern() { assertEquals("Certification",classifier.classify("Certificate of completion\nInternational Business")) }
    @Test fun courseCompletionNotInternship() { assertEquals("Certification",classifier.classify("Certificate of successful completion\nCourse: Data Systems")) }
    @Test fun contentWinsUploadHint() { assertEquals("Conference", classifier.classify(conference,"certificate.pdf","Certification")) }
    @Test fun workshopAttendanceClassified() { assertEquals("Conference",classifier.classify("Certificate of participation in a workshop")) }
    @Test fun explicitReviewCategoryCanOverride() { assertEquals("Publication",parse("Title: Example paper\nYear: 2026", "Publication").category) }
    @Test fun internshipStructuredFields() {
        val r=parse("""
            Internship Certificate
            Company: Example Networks Pvt. Ltd.
            Role: Network Intern
            Period: May 2026 - July 2026
            Tasks: Configured VLANs and VPNs.
            Maintained Windows Server and NAS systems.
        """); val i=r.items.single()
        assertEquals("Internship",i.type); assertEquals("Network Intern",i.title); assertEquals("Example Networks Pvt. Ltd",i.subtitle)
        assertEquals("May 2026 - July 2026",i.date);assertTrue(i.description.contains("Maintained Windows Server"))
        assertTrue(r.skills.map {it.name}.containsAll(listOf("VLAN","VPN","Windows Server","NAS")))
    }
    @Test fun internshipProseKeepsCompanyAbbreviation() {
        val i=parse("This certifies that Test Student completed an internship at Example Networks Pvt. Ltd. from May 2026 to July 2026. He worked with VPNs.").items.single()
        assertEquals("Example Networks Pvt. Ltd",i.subtitle); assertEquals("May 2026 – July 2026",i.date)
    }
    @Test fun issueDateNotEmploymentPeriod() { assertEquals("",parse("Internship Certificate\nCompany: Example\nIssued on: 1 August 2026").items.single().date) }
    @Test fun noInventedResponsibilities() { assertEquals("",parse("Internship certificate\nCompany: Example\nPeriod: June 2026 - July 2026").items.single().description) }
    @Test fun missingTitleIsAnEditableWarningNotMadeUpFact() { val r=parse("Certificate of Completion\nTest Student");assertEquals("",r.items.single().title);assertTrue(r.warnings.any {it.contains("title",true)}) }
    @Test fun blankInputWarnsWithoutInventedSkills() { val r=parse("","Certification"); assertTrue(r.skills.isEmpty()); assertTrue(r.warnings.isNotEmpty()) }
    @Test fun certificationIndependentOfSkills() { val r=parse("Certificate\nCourse: Classical Philosophy\nIssuer: Example\nDate: May 2026"); assertEquals(1,r.items.size); assertTrue(r.skills.isEmpty()) }
    @Test fun publicationAllRequestedFields() { val i=parse("Publication\nTitle: Efficient Cloud Scheduling\nPublisher: Example Press\nYear: 2026\nDOI: 10.1234/test.567", "Publication").items.single(); assertEquals("Efficient Cloud Scheduling",i.title);assertEquals("Example Press",i.subtitle);assertEquals("2026",i.date);assertEquals("https://doi.org/10.1234/test.567",i.link) }
    @Test fun acceptanceIsNotAlreadyPublished() { val i=parse("Paper title: Example research\nAccepted for publication\nPublisher: Example Press", "Publication").items.single();assertTrue(i.description.contains("not confirmed published")) }
    @Test fun presentationDoesNotBecomePublication() { assertEquals("Conference",classifier.classify("Certificate of presentation\nPresented a paper at the International Conference on AI")) }
    @Test fun explicitPublishedPaperCanBePublication() { assertEquals("Publication",classifier.classify("This paper was published in the International Conference proceedings")) }
    @Test fun aliasesMerged() { val s=intelligence.extract("RAG and Retrieval Augmented Generation; Amazon Web Services (AWS); Python Python", "test"); assertEquals(1,s.count {it.name.startsWith("Retrieval-")});assertEquals(1,s.count {it.name=="AWS"});assertEquals(1,s.count {it.name=="Python"}) }
    @Test fun wholeWordSkillsAvoidSubstringPollution() { val s=names("JavaScript and GitHub, international training.");assertTrue("JavaScript" in s);assertFalse("Java" in s);assertFalse("Git" in s) }
    @Test fun urlKeywordsAreNotSkills() { assertTrue(names("https://example.com/Python/RAG/PaddleOCR").isEmpty()) }
    @Test fun unknownLabelledTechnologyRetained() { assertTrue("ExampleVectorDB" in names("Tools: ExampleVectorDB, Python")) }
    @Test fun noCapOnExplicitSkills() { val s=names("Skills: Kotlin, Python, SQL, Docker, AWS, LangChain, LlamaIndex, FAISS, OpenCV, Pandas, NumPy, TensorFlow, Git, Java, Swift, Flask");assertTrue(s.size>=16) }
    @Test fun evidenceIncluded() { assertTrue(intelligence.extract("Worked with Python and SQL.","test").all {it.evidence.contains("Worked with")}) }
    @Test fun multilineCourseName() { val i=parse("Coursera\nhas successfully completed\nGenerative AI Applications\nwith RAG and LangChain\nan online course authorized by Example and offered through Coursera").items.single();assertEquals("Generative AI Applications with RAG and LangChain",i.title) }
    @Test fun multilineConferenceName() { val i=parse("Certificate of Participation\nInternational Conference on\nComputer Science and Engineering\nheld on 15 September 2026").items.single();assertEquals("International Conference on Computer Science and Engineering",i.title);assertEquals("September 2026",i.date) }
    @Test fun sameDocumentStableEntryId() { assertEquals(parse(course).items.single().id,parse(course).items.single().id) }
    @Test fun differentDocumentsDifferentIds() { assertFalse(DocumentExtractionService.stableId("one",0)==DocumentExtractionService.stableId("two",0)) }
    @Test fun sameFactsDuplicateCaseWhitespace() { val i=CareerItemEntity("a","Conference","ICSCES","Example",date="September 2026"); assertTrue(EntryIdentity.same(i,i.copy(id="b",title="  icsces  "))) }
    @Test fun annualConferenceNotDuplicateAcrossYears() { val i=CareerItemEntity("a","Conference","ICSCES","Example",date="September 2026"); assertFalse(EntryIdentity.same(i,i.copy(id="b",date="September 2025"))) }
    @Test fun safeBareWebsiteNormalized() { assertEquals("https://github.com/test",UrlPolicy.normalize("github.com/test")) }
    @Test fun dangerousUrlsRejected() { listOf("javascript:alert(1)","file:///sdcard/a","data:text/html,evil","https://user:pw@example.com","https://example.com/<script>").forEach {assertNull(UrlPolicy.normalize(it))} }
    @Test fun validDoiNormalized() { assertEquals("https://doi.org/10.1234/example",UrlPolicy.normalize("10.1234/example")) }
    @Test fun unsupportedSingleWordsNotLinks() { assertNull(UrlPolicy.normalize("hello")) }
    @Test fun htmlConferenceAndCertificateExistWithoutSkills() { val html=portfolio(parse(course).items+parse(conference).items);assertTrue(html.contains("<h2>Certifications</h2>"));assertTrue(html.contains("<h2>Conferences &amp; Seminars</h2>"));assertFalse(html.contains("<h2>Skills</h2>")) }
    @Test fun htmlPublicationHasClickableLinkAndPublisher() { val html=portfolio(listOf(CareerItemEntity("p","Publication","Research Topic","Example Press",date="2026",link="https://doi.org/10.1234/example")));assertTrue(html.contains("Read publication"));assertTrue(html.contains("href=\"https://doi.org/10.1234/example\""));assertTrue(html.contains("Example Press · 2026")) }
    @Test fun allSectionsRenderedIndependently() { val html=portfolio(CareerType.values().map {CareerItemEntity(it.label,it.label,"Example ${it.label}")});CareerType.values().forEach {assertTrue(html.contains("data-section=\"${it.label}\""))} }
    @Test fun maliciousTextEscapedInExport() { val html=portfolio(listOf(CareerItemEntity("p","Publication","<script>alert(1)</script>",link="javascript:alert(2)")));assertFalse(html.contains("<script>"));assertTrue(html.contains("&lt;script&gt;"));assertFalse(html.contains("href=\"javascript")) }
    @Test fun privateEvidenceNotInHtml() { val s=Snapshot(ProfileEntity(name="Student"), listOf(DocumentEntity("d","secret.pdf","/private/path","application/pdf",verifiedText="PRIVATE SOURCE TEXT")),emptyList(),listOf(SkillEntity("s","Python","Programming",evidence="PRIVATE SKILL EVIDENCE")));val html=PortfolioRenderer.render(s);assertFalse(html.contains("PRIVATE SOURCE TEXT"));assertFalse(html.contains("PRIVATE SKILL EVIDENCE"));assertFalse(html.contains("/private/path")) }
    @Test fun descriptionsNotSilentlyTruncated() { val text="Verified responsibility. ".repeat(100);val html=portfolio(listOf(CareerItemEntity("a","Internship","Intern",description=text)));assertTrue(html.contains(text)) }
    @Test fun datePrecisionNotInvented() { assertEquals("2026",EvidenceDates().monthYear("2026"));assertEquals("03/04/2026",EvidenceDates().monthYear("03/04/2026")) }
    @Test fun titleYearNotMistakenForWorkPeriod() { assertEquals("",EvidenceDates().period("Internship completed. Issued 2026.")) }
    @Test fun unknownSourceDoesNotInventEntry() { val r=parse("Hello from a plain document");assertTrue(r.items.isEmpty());assertTrue(r.warnings.isNotEmpty()) }
    @Test fun fullEventDatePreferredToTitleYear() { assertEquals("October 2026",EvidenceDates().monthYear(EvidenceDates().eventDate("ICSCES 2026\n25 October 2026"))) }

}
