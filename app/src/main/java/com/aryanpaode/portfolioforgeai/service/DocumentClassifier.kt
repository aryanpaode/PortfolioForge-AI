package com.aryanpaode.portfolioforgeai.service

/** Document content wins over the upload category. Never match 'intern' inside 'International'. */
class DocumentClassifier {
    fun classify(text: String, fileName: String = "", hint: String = "Auto detect"): String {
        fun has(pattern: String) = Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(text)
        val headings = listOf("experience", "education", "skills", "projects", "certifications")
            .count { has("^\\s*$it\\s*:?\\s*$") }
        if (headings >= 2) return if (has("\\blinkedin\\b")) "LinkedIn Profile" else "Resume"
        if (has("\\b(internship|internships|intern|trainee|work experience|employment certificate|experience letter)\\b")) return "Internship"
        // A paper's publication is different from attendance, presentation or acceptance.
        val paper = has("\\b(paper|article|manuscript|journal|publication)\\b")
        if (paper && has("\\b(published|publication certificate)\\b")) return "Publication"
        if (has("\\b(conference|symposium|seminar|webinar|congress)\\b") ||
            (has("\\bworkshop\\b") && has("\\b(participat|attend)(?:ed|ion|ant|ance)?\\b"))) return "Conference"
        if (paper && has("\\b(doi|issn|abstract|accepted for publication)\\b")) return "Publication"
        if (has("\\b(winner|runner.up|first prize|second prize|award|achievement|ranked)\\b")) return "Achievement"
        if (has("\\b(certificate|certification|certify|successfully completed|coursera|udemy|nptel)\\b")) return "Certification"
        if (has("\\b(mark ?sheet|transcript|degree awarded|bachelor of|master of)\\b")) return "Education"
        if (has("\\b(project report|project title|developed|implemented)\\b")) return "Project"
        if (hint !in listOf("Auto detect", "Other", "")) return hint
        // Filenames are only weak classification hints, never skill evidence.
        return when {
            Regex("(?i)\\bresume\\b").containsMatchIn(fileName) -> "Resume"
            Regex("(?i)\\binternship\\b").containsMatchIn(fileName) -> "Internship"
            Regex("(?i)\\b(conference|seminar)\\b").containsMatchIn(fileName) -> "Conference"
            Regex("(?i)\\bcertificate\\b").containsMatchIn(fileName) -> "Certification"
            else -> "Other"
        }
    }
}
