package com.aryanpaode.portfolioforgeai.service

import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import com.aryanpaode.portfolioforgeai.data.model.DocumentEntity
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import com.aryanpaode.portfolioforgeai.data.model.ReadinessResult
import com.aryanpaode.portfolioforgeai.data.model.SkillEntity

class ReadinessEngine {
    fun calculate(
        profile: ProfileEntity?,
        items: List<CareerItemEntity>,
        skills: List<SkillEntity>,
        documents: List<DocumentEntity>
    ): ReadinessResult {
        if (profile == null) return ReadinessResult(0, listOf("Complete your student profile."))

        var score = 0
        val suggestions = mutableListOf<String>()

        if (profile.name.isNotBlank() && profile.email.isNotBlank() && profile.phone.isNotBlank()) score += 15
        else suggestions += "Complete your name, email and phone details."

        if (profile.branch.isNotBlank() && profile.semester.isNotBlank() && profile.cgpa.isNotBlank()) score += 15
        else suggestions += "Add branch, semester and CGPA."

        if (profile.githubUrl.isNotBlank()) score += 10 else suggestions += "Add your GitHub profile link."
        if (profile.linkedInUrl.isNotBlank()) score += 5 else suggestions += "Add your LinkedIn/profile link."
        if (profile.targetRole.isNotBlank() && profile.summary.length >= 40) score += 10
        else suggestions += "Add a target role and a concise professional summary."

        val projects = items.filter { it.type == "Project" }
        if (projects.size >= 2) score += 15 else suggestions += "Add at least two strong projects."
        if (projects.any { it.description.any(Char::isDigit) || it.description.contains("%") }) score += 5
        else if (projects.isNotEmpty()) suggestions += "Add measurable outcomes to at least one project."

        if (skills.size >= 5) score += 10 else suggestions += "Add or verify at least five relevant technical skills."
        if (skills.map { it.category }.distinct().size >= 2) score += 5 else suggestions += "Group skills across relevant categories."

        if (documents.any { it.verified }) score += 5 else suggestions += "Verify at least one OCR-extracted document."
        if (items.any { it.type == "Certification" } || documents.any { it.category == "Certification" }) score += 5
        else suggestions += "Add at least one certification or achievement proof."

        return ReadinessResult(score.coerceIn(0, 100), suggestions.distinct().take(5))
    }
}
