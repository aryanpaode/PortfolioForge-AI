package com.aryanpaode.portfolioforgeai.service

import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity

object EntryIdentity {
    private fun n(value: String) = value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    /** Year and organization are part of identity: recurring conferences/internships stay distinct. */
    fun key(item: CareerItemEntity) = listOf(item.type, item.title, item.subtitle, item.date).joinToString("|") { n(it) }
    fun same(a: CareerItemEntity, b: CareerItemEntity): Boolean =
        a.id == b.id || key(a) == key(b) ||
            (a.type == b.type && a.link.isNotBlank() && UrlPolicy.normalize(a.link) != null &&
                UrlPolicy.normalize(a.link) == UrlPolicy.normalize(b.link) && n(a.title) == n(b.title) && n(a.date) == n(b.date))
}
