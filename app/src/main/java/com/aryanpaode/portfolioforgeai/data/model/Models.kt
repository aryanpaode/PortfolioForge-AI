package com.aryanpaode.portfolioforgeai.data.model

enum class CareerType(val label: String, val heading: String) {
    PROJECT("Project", "Projects"),
    INTERNSHIP("Internship", "Experience & Internships"),
    CERTIFICATION("Certification", "Certifications"),
    CONFERENCE("Conference", "Conferences & Seminars"),
    PUBLICATION("Publication", "Publications"),
    ACHIEVEMENT("Achievement", "Achievements"),
    EDUCATION("Education", "Education");

    companion object {
        fun heading(type: String): String = values().firstOrNull { it.label == type }?.heading ?: type
        val documentCategories = listOf("Auto detect", "Certification", "Internship", "Conference", "Publication", "Project", "Achievement", "Education", "Resume", "LinkedIn Profile", "Other")
    }
}

data class ReadinessResult(val score: Int, val suggestions: List<String>)
