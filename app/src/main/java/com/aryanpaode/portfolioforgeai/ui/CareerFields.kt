package com.aryanpaode.portfolioforgeai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import com.aryanpaode.portfolioforgeai.data.model.CareerType

@Composable
fun CareerFields(item: CareerItemEntity, onChange: (CareerItemEntity) -> Unit, allowTypeChange: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (allowTypeChange) ChoicePicker("Section", item.type, CareerType.values().map { it.label }) { onChange(item.copy(type = it)) }
        val title = when (item.type) {
            "Publication" -> "Publication topic / title"
            "Conference" -> "Conference / seminar name"
            "Internship" -> "Role / position"
            "Certification" -> "Course / certification title"
            "Education" -> "Qualification / degree"
            else -> "Title"
        }
        val organization = when (item.type) {
            "Publication" -> "Publisher"
            "Internship" -> "Company / employer"
            "Conference" -> "Organizer (optional)"
            "Certification" -> "Issuer / platform"
            "Education" -> "Institution / university"
            else -> "Organization / technologies (optional)"
        }
        val period = when (item.type) {
            "Publication" -> "Year (YYYY)"
            "Internship" -> "Period (start – end)"
            "Conference", "Certification" -> "Month / year"
            else -> "Date / duration (optional)"
        }
        AppTextField(item.title, { onChange(item.copy(title = it)) }, title, singleLine = item.type == "Internship", minLines = if (item.type == "Internship") 1 else 2)
        AppTextField(item.subtitle, { onChange(item.copy(subtitle = it)) }, organization)
        AppTextField(item.date, { onChange(item.copy(date = it)) }, period, keyboardType = if (item.type == "Publication") KeyboardType.Number else KeyboardType.Text)
        AppTextField(item.link, { onChange(item.copy(link = it)) }, if (item.type == "Publication") "Publication link / DOI" else "Link (optional)", allowPaste = true, keyboardType = KeyboardType.Uri)
        AppTextField(item.description, { onChange(item.copy(description = it)) }, when (item.type) {
            "Internship" -> "Tasks / responsibilities (only verified details)"
            "Conference" -> "Participation / role / notes"
            "Publication" -> "Notes / status (optional)"
            else -> "Description (optional)"
        }, singleLine = false, minLines = 3)
    }
}
