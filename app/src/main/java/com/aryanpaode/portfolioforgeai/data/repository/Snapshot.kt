package com.aryanpaode.portfolioforgeai.data.repository

import com.aryanpaode.portfolioforgeai.data.model.*

/** Immutable, single-workspace input shared by previews and both exporters. */
data class Snapshot(
    val profile: ProfileEntity?,
    val documents: List<DocumentEntity>,
    val items: List<CareerItemEntity>,
    val skills: List<SkillEntity>,
    val workspaceId: String = "local",
    val loaded: Boolean = true
) {
    companion object {
        fun empty(workspaceId: String, loaded: Boolean = false) = Snapshot(null, emptyList(), emptyList(), emptyList(), workspaceId, loaded)
    }
}
