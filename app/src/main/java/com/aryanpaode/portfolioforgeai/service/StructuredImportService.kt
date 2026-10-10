package com.aryanpaode.portfolioforgeai.service

import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import com.aryanpaode.portfolioforgeai.data.model.SkillEntity
import java.util.UUID

data class StructuredImportPreview(
    val sourceName: String,
    val profile: ProfileEntity,
    val skills: List<SkillEntity>,
    val items: List<CareerItemEntity>
)

class StructuredImportService(private val skillIntelligence: SkillIntelligence) {

    private val emailRegex = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("(?<!\\d)(?:\\+?91[-\\s]?)?[6-9]\\d{9}(?!\\d)")
    private val urlRegex = Regex("https?://[^\\s]+|www\\.[^\\s]+", RegexOption.IGNORE_CASE)
    private val yearRegex = Regex("\\b(?:19|20)\\d{2}(?:\\s*[-–]\\s*(?:Present|Current|(?:19|20)\\d{2}))?\\b", RegexOption.IGNORE_CASE)

    private val sectionMap = linkedMapOf(
        "projects" to "Project",
        "project" to "Project",
        "work experience" to "Internship",
        "experience" to "Internship",
        "internships" to "Internship",
        "internship" to "Internship",
        "certifications" to "Certification",
        "certification" to "Certification",
        "certificates" to "Certification",
        "achievements" to "Achievement",
        "achievement" to "Achievement",
        "awards" to "Achievement",
        "publications" to "Publication",
        "publication" to "Publication",
        "education" to "Education",
        "conferences" to "Conference",
        "conferences and seminars" to "Conference",
        "conferences seminars" to "Conference",
        "seminars" to "Conference"
    )

    fun parse(text: String, sourceName: String): StructuredImportPreview {
        val lines = text.lines().map { it.trim() }
        val nonBlank = lines.filter { it.isNotBlank() }

        val email = emailRegex.find(text)?.value.orEmpty()
        val phone = phoneRegex.find(text)?.value.orEmpty()
        val urls = urlRegex.findAll(text).map { cleanUrl(it.value) }.toList()
        val github = urls.firstOrNull { it.contains("github.com", ignoreCase = true) }.orEmpty()
        val linkedIn = urls.firstOrNull { it.contains("linkedin.com", ignoreCase = true) }.orEmpty()
        val name = detectName(nonBlank, email, phone)
        val branch = detectEducationLine(nonBlank)

        val profile = ProfileEntity(
            name = name,
            email = email,
            phone = phone,
            branch = branch,
            githubUrl = github,
            linkedInUrl = linkedIn
        )

        val skills = skillIntelligence.extract(text, "Smart import: $sourceName")
        val items = extractCareerItems(lines, sourceName)

        return StructuredImportPreview(
            sourceName = sourceName,
            profile = profile,
            skills = skills,
            items = items
        )
    }

    private fun detectName(lines: List<String>, email: String, phone: String): String {
        return lines.take(10).firstOrNull { line ->
            val words = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            words.size in 2..4 &&
                line.length in 4..55 &&
                line.none { it.isDigit() } &&
                !line.contains("@") &&
                !line.contains("http", ignoreCase = true) &&
                !line.equals(email, ignoreCase = true) &&
                (phone.isBlank() || !line.contains(phone)) &&
                sectionType(line) == null &&
                line.count { it.isLetter() } >= line.length / 2
        }.orEmpty()
    }

    private fun detectEducationLine(lines: List<String>): String {
        val hints = listOf(
            "b.tech", "btech", "bachelor", "computer engineering", "computer science",
            "information technology", "engineering", "b.e.", "bsc", "m.tech", "master"
        )
        return lines.firstOrNull { line -> hints.any { line.contains(it, ignoreCase = true) } }
            
            .orEmpty()
    }

    private fun extractCareerItems(lines: List<String>, sourceName: String): List<CareerItemEntity> {
        val sections = linkedMapOf<String, MutableList<String>>()
        var currentType: String? = null

        lines.forEach { line ->
            val detected = sectionType(line)
            if (detected != null) {
                currentType = detected
                sections.getOrPut(detected) { mutableListOf() }
            } else if (line.lowercase().trim().removeSuffix(":") in setOf("skills", "technical skills", "summary", "profile", "contact", "languages", "interests")) {
                currentType = null
            } else if (currentType != null) {
                sections.getOrPut(currentType!!) { mutableListOf() }.add(line)
            }
        }

        return sections.flatMap { (type, sectionLines) ->
            parseSection(type, sectionLines, sourceName)
        }.distinctBy { "${it.type}|${it.title}".lowercase() }
    }

    private fun parseSection(type: String, lines: List<String>, sourceName: String): List<CareerItemEntity> {
        val trimmed = lines.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        if (trimmed.isEmpty()) return emptyList()

        val blocks = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        trimmed.forEach { line ->
            if (line.isBlank()) {
                if (current.isNotEmpty()) {
                    blocks += current.toList()
                    current = mutableListOf()
                }
            } else {
                current += line.removePrefix("•").removePrefix("-").trim()
            }
        }
        if (current.isNotEmpty()) blocks += current.toList()

        // Blank-separated records stay intact; do not turn organization/date lines into new certificates.
        val normalizedBlocks = blocks.ifEmpty { listOf(trimmed.filter { it.isNotBlank() }) }


        return normalizedBlocks.mapNotNull { block ->
            if (block.isEmpty()) return@mapNotNull null
            val first = block.first().trim()
            if (first.isBlank()) return@mapNotNull null

            val link = block.asSequence().mapNotNull { urlRegex.find(it)?.value }.firstOrNull()?.let(::cleanUrl).orEmpty()
            val date = block.asSequence().mapNotNull { yearRegex.find(it)?.value }.firstOrNull().orEmpty()
            val split = first.split(Regex("\\s+[|–-]\\s+"), limit = 2)
            val title = split.first()
            val subtitle = split.getOrNull(1).orEmpty()
            val description = block.drop(1)
                .filterNot { it == link || it == date }
                .joinToString(" ")
                

            CareerItemEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                title = title,
                subtitle = subtitle,
                description = description,
                date = date,
                link = link
            )
        }
    }

    private fun sectionType(line: String): String? {
        val normalized = line.lowercase()
            .replace(Regex("[^a-z ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return sectionMap.entries.firstOrNull { (heading, _) ->
            normalized == heading || normalized == "$heading section"
        }?.value
    }

    private fun cleanUrl(value: String): String {
        val clean = value.trim().trimEnd('.', ',', ';', ')', ']')
        return if (clean.startsWith("www.", ignoreCase = true)) "https://$clean" else clean
    }
}
