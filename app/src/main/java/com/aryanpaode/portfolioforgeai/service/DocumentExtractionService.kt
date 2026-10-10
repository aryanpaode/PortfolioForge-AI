package com.aryanpaode.portfolioforgeai.service

import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import com.aryanpaode.portfolioforgeai.data.model.SkillEntity
import java.util.UUID

/** A review draft. Nothing is written to the profile merely by running extraction. */
data class DocumentReview(
    val documentId: String,
    val category: String,
    val text: String,
    val items: List<CareerItemEntity>,
    val skills: List<SkillEntity>,
    val warnings: List<String> = emptyList(),
    val profile: ProfileEntity? = null,
    val skillsAreTopics: Boolean = false
)

/**
 * Deterministic document-aware extraction after local text extraction/OCR.
 * It does NOT call an LLM, scrape a course website or invent missing responsibilities.
 * All proposed fields, selected skills and the destination section are editable in review.
 */
class DocumentExtractionService(
    private val skills: SkillIntelligence,
    private val classifier: DocumentClassifier,
    private val resumeImport: StructuredImportService
) {
    fun parse(documentId: String, text: String, fileName: String, category: String = "Auto detect"): DocumentReview {
        val normalized = text.replace('\u00a0', ' ').replace("\r\n", "\n").replace('\r', '\n')
        val type = if (category == "Auto detect") classifier.classify(normalized, fileName) else category
        val warnings = mutableListOf<String>()
        if (normalized.isBlank()) warnings += "No readable text was found. Enter the missing details or paste corrected text; do not accept empty fields as extracted facts."
        val sourceSkills = skills.extract(normalized, "Document: $fileName")
        if (type in listOf("Resume", "LinkedIn Profile")) {
            val imported = resumeImport.parse(normalized, fileName)
            return DocumentReview(documentId, type, normalized,
                imported.items.mapIndexed { index, item -> item.copy(id = stableId(documentId, index), sourceDocumentId = documentId) },
                sourceSkills, warnings + "Resume sections are heuristically separated. Check every entry before saving.", imported.profile)
        }
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        val flat = lines.joinToString(" ")
        val dateParser = EvidenceDates()
        fun field(vararg labels: String): String = labelled(lines, labels.toList())
        val urls = UrlPolicy.extract(normalized)
        var title = ""
        var organization = ""
        var date = ""
        var description = ""
        var link = urls.firstOrNull().orEmpty()

        when (type) {
            "Internship" -> {
                organization = field("Company", "Employer", "Organisation", "Organization")
                if (organization.isBlank()) organization = firstCapture(flat,
                    "\\b(?:internship|intern|trainee|employed|worked)(?:\\s+(?:program(?:me)?|training))?\\s+(?:at|with|in)\\s+(?:the\\s+)?(.+?)(?=\\s+(?:from|during|for a|for the|between|as an?|on|where|and worked)\\b|;|\\.(?=\\s+(?:He|She|They|This|The intern)\\b)|$)")
                if (organization.isBlank()) organization = lines.firstOrNull { line ->
                    line.length <= 160 && Regex("(?i)\\b(?:pvt\\.?\\s*ltd\\.?|private limited|limited|ltd\\.?|llp|inc\\.?|corporation)\\b").containsMatchIn(line) &&
                        !Regex("(?i)\\b(this|certify|completed|worked|internship)\\b").containsMatchIn(line)
                }.orEmpty()
                title = field("Role", "Position", "Designation", "Internship role")
                if (title.isBlank()) title = firstCapture(flat, "\\b(?:as an?|position of)\\s+([A-Za-z][A-Za-z /&-]{1,65}?(?:intern|trainee|engineer|analyst|developer|assistant))\\b")
                if (title.isBlank() && Regex("(?i)\\bintern(?:ship)?\\b").containsMatchIn(flat)) title = "Intern"
                date = field("Period", "Duration", "Internship period", "Internship duration", "Employment period")
                if (date.isBlank()) date = dateParser.period(normalized)
                description = tasks(lines)
                if (organization.isBlank()) warnings += "Company/employer was not confidently identified. Add it from the certificate."
                if (date.isBlank()) warnings += "The employment/internship period is missing. An issue date is not treated as the work period."
                if (description.isBlank()) warnings += "No tasks or responsibilities were stated. Leave this blank or add your own verified description."
            }
            "Conference" -> {
                title = field("Conference name", "Name of conference", "Event name", "Seminar name", "Conference", "Event", "Seminar", "Webinar", "Workshop")
                if (title.isBlank()) title = conferenceTitle(lines)
                organization = field("Organizer", "Organiser", "Organized by", "Organised by", "Hosted by")
                if (organization.isBlank()) organization = firstCapture(flat, "\\b(?:organised|organized|hosted) by\\s+(.+?)(?=\\s+(?:on|held|during|from|at the|in association)\\b|[.;]|$)")
                date = field("Event date", "Conference date", "Month/year", "Month / year")
                if (date.isBlank()) date = dateParser.eventDate(normalized)
                date = dateParser.monthYear(date)
                description = when {
                    Regex("(?i)\\b(presented|paper presentation|presenter)\\b").containsMatchIn(normalized) -> "Paper presenter"
                    Regex("(?i)\\b(keynote speaker|invited speaker)\\b").containsMatchIn(normalized) -> "Invited speaker"
                    Regex("(?i)\\b(participated|participation|participant)\\b").containsMatchIn(normalized) -> "Participant"
                    Regex("(?i)\\b(attended|attendance|attendee)\\b").containsMatchIn(normalized) -> "Attendee"
                    else -> ""
                }
                warnings += "Conference subjects are topic suggestions, not proof of proficiency. They start unchecked; select only skills you can substantiate."
                if (date.isBlank()) warnings += "No event month/year was found. Do not use the certificate's issue date unless it is also the event date."
            }
            "Certification" -> {
                title = field("Course title", "Course name", "Course", "Certification title", "Certificate title", "Title")
                if (title.isBlank()) title = courseTitle(lines)
                organization = field("Issuer", "Issued by", "Publisher", "Awarded by", "Authorized by", "Authorised by")
                val authorized = firstCapture(flat, "\\bauthorized by\\s+(.+?)(?=\\s+and offered|\\s+offered|[.;]|$)")
                if (organization.isBlank()) organization = authorized
                val platform = listOf("Coursera", "Udemy", "NPTEL", "edX", "LinkedIn Learning", "Great Learning", "freeCodeCamp")
                    .firstOrNull { Regex("(?i)(?<![A-Za-z])${Regex.escape(it)}(?![A-Za-z])").containsMatchIn(normalized) }
                if (platform != null && !organization.contains(platform, true)) organization = listOf(organization, platform).filter { it.isNotBlank() }.joinToString(" / ")
                date = field("Completion date", "Date of completion", "Issue date", "Issued on", "Date")
                if (date.isBlank()) date = dateParser.certificateDate(normalized)
                date = dateParser.monthYear(date)
                description = if (Regex("(?i)\\b(completed|completion)\\b").containsMatchIn(normalized)) "Course completed" else ""
                link = urls.firstOrNull { it.contains("verify", true) || it.contains("certificate", true) || it.contains("credential", true) } ?: link
            }
            "Publication" -> {
                title = field("Publication topic", "Publication title", "Paper title", "Article title", "Title", "Topic")
                if (title.isBlank()) title = firstCapture(flat, "\\b(?:paper|article|manuscript) (?:entitled|titled)\\s+[\"“]?(.+?)(?=[\"”]|\\s+(?:has been|was|is|published|accepted)\\b|$)")
                if (title.isBlank()) title = lines.takeWhile { !it.contains("abstract", true) }.firstOrNull {
                    it.length in 25..260 && !boilerplate(it) && !Regex("(?i)\\b(doi|issn|volume|authors?|publisher|certificate|copyright)\\b").containsMatchIn(it)
                }.orEmpty()
                organization = field("Publisher", "Published by", "Journal", "Publication venue")
                if (organization.isBlank()) organization = firstCapture(flat, "\\bpublished (?:by|in)\\s+(.+?)(?=\\s+(?:in (?:19|20)\\d{2}|on|volume|vol\\.|with doi)\\b|[;]|$)")
                val yearSource = field("Publication year", "Year", "Publication date")
                date = Regex("\\b(?:19|20)\\d{2}\\b").find(yearSource.ifBlank { normalized })?.value.orEmpty()
                link = urls.firstOrNull { it.contains("doi.org", true) } ?: link
                if (Regex("(?i)\\baccepted\\b").containsMatchIn(normalized) && !Regex("(?i)\\bpublished\\b").containsMatchIn(normalized)) {
                    description = "Accepted for publication (not confirmed published)"
                    warnings += "This document indicates acceptance, not completed publication. Verify the publication year and status."
                }
            }
            "Project" -> {
                title = field("Project title", "Project name", "Title")
                organization = field("Organization", "Organisation", "Institution", "Technologies", "Technology")
                description = field("Description", "Summary", "Objective")
                if (description.isBlank()) description = tasks(lines)
                date = field("Period", "Duration", "Date", "Year")
                link = urls.firstOrNull { it.contains("github.com", true) || it.contains("gitlab.com", true) } ?: link
            }
            "Achievement" -> {
                title = field("Award", "Achievement", "Competition", "Title")
                if (title.isBlank()) title = lines.firstOrNull { Regex("(?i)\\b(winner|prize|runner.up|award|ranked)\\b").containsMatchIn(it) && !boilerplate(it) }.orEmpty()
                organization = field("Awarded by", "Organizer", "Organiser", "Institution")
                date = dateParser.monthYear(dateParser.certificateDate(normalized))
            }
            "Education" -> {
                title = field("Qualification", "Degree", "Program", "Programme", "Course")
                if (title.isBlank()) title = lines.firstOrNull { Regex("(?i)\\b(bachelor|master|b\\.?tech|b\\.?e\\.|diploma|b\\.?sc)\\b").containsMatchIn(it) }.orEmpty()
                organization = field("Institution", "University", "College", "School")
                date = field("Period", "Academic year", "Year")
            }
            else -> warnings += "Choose a destination section, or save only the verified text. No career facts were invented."
        }
        title = clean(title); organization = clean(organization); date = clean(date)
        if (title.isBlank() && type != "Other") warnings += "The title was not confidently found. Enter it before selecting this section entry for saving."
        if (date.isNotBlank() && Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b").containsMatchIn(date)) warnings += "Check the numeric date format against the original certificate."
        val items = if (type in listOf("Internship", "Certification", "Conference", "Publication", "Project", "Achievement", "Education")) listOf(
            CareerItemEntity(stableId(documentId, 0), type, title, organization, description.trim(), date, link, documentId)
        ) else emptyList()
        return DocumentReview(documentId, type, normalized, items, sourceSkills, warnings.distinct(), skillsAreTopics = type == "Conference")
    }

    private fun clean(value: String) = value.trim().trim('"', '“', '”', ':', ' ', '.', ',').replace(Regex("\\s+"), " ")
    private fun firstCapture(text: String, regex: String): String = Regex(regex, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim().orEmpty()
    private fun labelled(lines: List<String>, names: List<String>): String {
        val label = names.joinToString("|") { Regex.escape(it) }
        val p = Regex("(?i)^\\s*(?:$label)\\s*[:：]\\s*(.*)$")
        lines.forEachIndexed { i, line ->
            val found = p.find(line)
            if (found != null) return found.groupValues[1].trim().ifBlank { lines.getOrNull(i + 1).orEmpty() }
        }
        return ""
    }
    private fun boilerplate(line: String) = Regex("(?i)^(?:certificate|this is|this certifies|certify that|awarded to|presented to|has successfully|successfully completed|date\\b|signature|verify|https?://|www\\.|copyright|issued on|authorized signatory)").containsMatchIn(line)

    private fun courseTitle(lines: List<String>): String {
        lines.forEachIndexed { i, line ->
            val m = Regex("(?i)\\b(?:successfully\\s+)?completed(?:\\s+(?:the|an?|online))*\\s*(?:course(?:\\s+on|\\s+in)?\\s*[:：]?\\s*)?(.*)$").find(line)
            if (m != null) {
                val same = m.groupValues[1].trim().trim('"', '“', '”')
                    .split(Regex("(?i)\\s+(?:on\\s+\\d|an online|authorized by|offered through|from\\s+Coursera|with a grade)"))[0].trim()
                if (same.length >= 4 && !same.equals("course", true)) return same
                val next = mutableListOf<String>()
                for (j in i + 1..minOf(i + 4, lines.lastIndex)) {
                    val candidate = lines[j]
                    if (Regex("(?i)^(?:an online|a course|authorized|offered|provided|through|date|verify|https?://|www\\.|this|signature|instructor)").containsMatchIn(candidate)) break
                    if (!boilerplate(candidate) && !EvidenceDates().isOnlyDate(candidate)) next += candidate
                }
                if (next.isNotEmpty()) return next.joinToString(" ")
            }
        }
        return lines.firstOrNull { it.length > 15 && !boilerplate(it) && skills.extract(it, "").isNotEmpty() && !EvidenceDates().isOnlyDate(it) }.orEmpty()
    }

    private fun conferenceTitle(lines: List<String>): String {
        val event = Regex("(?i)\\b(?:(?:\\d+(?:st|nd|rd|th)?|annual|international|national|regional|world|global)\\s+)*(?:conference|symposium|seminar|webinar|workshop|congress)\\b")
        val stop = Regex("(?i)\\s+(?:held|organised|organized|hosted|scheduled|conducted|from|during|on\\s+(?:\\d|Monday|Tuesday|Wednesday|Thursday|Friday))\\b")
        val candidates = mutableListOf<String>()
        lines.forEachIndexed { index, line ->
            val match = event.find(line) ?: return@forEachIndexed
            var candidate = line.substring(match.range.first).split(stop)[0].trim()
            if (Regex("(?i)^(?:conference|seminar|workshop)\\s+(?:participation|certificate|date|name)$").matches(candidate)) return@forEachIndexed
            if (candidate.endsWith(" on", true) || candidate.endsWith(" in", true) || candidate.endsWith(" and", true)) {
                for (j in index + 1..minOf(index + 3, lines.lastIndex)) {
                    if (Regex("(?i)^(?:held|organised|organized|hosted|from|on\\s|date|certificate|this |awarded|participant|presented|issued|signature|https?://)").containsMatchIn(lines[j])) break
                    candidate += " " + lines[j].split(stop)[0]
                    if (!candidate.endsWith(" and", true) && !candidate.endsWith(" on", true)) break
                }
            }
            if (candidate.length >= 12) candidates += candidate
        }
        return candidates.maxByOrNull { candidate ->
            candidate.length.coerceAtMost(250) + if (candidate.contains("conference on", true) || candidate.contains("international", true)) 100 else 0
        } ?: firstCapture(lines.joinToString(" "), "\\b(?:participat(?:ed|ing) in|attend(?:ed|ing)) (?:the )?(.+?)(?=\\s+(?:held|on|organised|organized|during)\\b|[.;]|$)")
    }

    private fun tasks(lines: List<String>): String {
        val results = mutableListOf<String>()
        var inTasks = false
        val start = Regex("(?i)^(?:tasks|responsibilities|work performed|work undertaken|duties|activities|work description)\\s*[:：]\\s*(.*)$")
        lines.forEach { line ->
            val heading = start.find(line)
            if (heading != null) {
                inTasks = true
                if (heading.groupValues[1].isNotBlank()) results += heading.groupValues[1]
            } else if (inTasks && Regex("(?i)^(?:date|issued|signature|authorized|we wish|regards|sincerely|certificate|duration|period|company|role)\\b").containsMatchIn(line)) {
                inTasks = false
            } else if (inTasks) results += line
            else {
                val sentence = Regex("(?i)(?:\\b(?:he|she|they|the intern|the trainee)\\s+)?\\b(?:worked on|worked with|developed|implemented|assisted with|configured|maintained|contributed to|was responsible for)\\b[^\n]*(?:$)").find(line)?.value
                if (!sentence.isNullOrBlank()) results += sentence.trim()
            }
        }
        return results.distinct().joinToString("\n")
    }

    companion object {
        fun stableId(documentId: String, index: Int) = UUID.nameUUIDFromBytes("$documentId|entry|$index".toByteArray()).toString()
    }
}

/** Preserve source precision. Ambiguous numeric dates are left as written, never guessed. */
class EvidenceDates {
    private val month = "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)"
    private val date = "(?:\\d{1,2}(?:st|nd|rd|th)?\\s+$month[,.]?\\s+(?:19|20)\\d{2}|$month\\s+\\d{1,2}(?:st|nd|rd|th)?[,]?\\s+(?:19|20)\\d{2}|$month[,.]?\\s+(?:19|20)\\d{2}|(?:19|20)\\d{2}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/-]\\d{1,2}[/-](?:19|20)\\d{2}|\\d{1,2}[/-](?:19|20)\\d{2}|(?:19|20)\\d{2})"
    private val pattern = Regex("(?i)\\b$date\\b")
    fun isOnlyDate(text: String) = pattern.matches(text.trim())
    fun period(text: String): String {
        val range = Regex("(?i)\\b(?:from\\s+|between\\s+)?($date)\\s*(?:to|till|until|through|and|[-–—])\\s*($date|present|current)\\b").find(text)
        if (range != null) return "${range.groupValues[1]} – ${range.groupValues[2]}"
        return ""
    }
    fun eventDate(text: String): String {
        val after = Regex("(?i)\\b(?:held|conducted|scheduled|organised|organized)(?:\\s+(?:on|during|from))?\\s+($date)").find(text)?.groupValues?.get(1)
        if (!after.isNullOrBlank()) return after
        val onDate = Regex("(?i)\\bon\\s+($date)").findAll(text).firstOrNull { m ->
            !text.substring(maxOf(0, m.range.first - 15), m.range.first).contains("issued", true)
        }?.groupValues?.get(1)
        if (!onDate.isNullOrBlank()) return onDate
        // A title year alone is valid year precision, but is not an invented month.
        val evidence = text.lines().filterNot { Regex("(?i)\\b(issued|issue date|signature|copyright)\\b").containsMatchIn(it) }.joinToString("\n")
        return pattern.findAll(evidence).map { it.value }.firstOrNull { !Regex("(?:19|20)\\d{2}").matches(it) } ?: pattern.find(evidence)?.value.orEmpty()
    }
    fun certificateDate(text: String): String {
        val associated = Regex("(?i)\\b(?:completed on|completion date|issued on|issue date|date)\\s*[:：]?\\s*($date)").find(text)
        return associated?.groupValues?.get(1) ?: pattern.find(text)?.value.orEmpty()
    }
    fun monthYear(value: String): String {
        val textMonth = Regex("(?i)\\b($month)(?:[,.]?\\s+(?:\\d{1,2}(?:st|nd|rd|th)?[,]?\\s+)?)(\\d{4})\\b").find(value)
        if (textMonth != null) {
            val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            val name = months.firstOrNull { it.take(3).equals(textMonth.groupValues[1].take(3), true) } ?: textMonth.groupValues[1]
            return "$name ${textMonth.groupValues[2]}"
        }
        val iso = Regex("^(\\d{4})-(\\d{1,2})(?:-\\d{1,2})?$").find(value.trim())
        if (iso != null && iso.groupValues[2].toInt() in 1..12) return "${iso.groupValues[2].padStart(2, '0')}/${iso.groupValues[1]}"
        return value.trim()
    }
}
