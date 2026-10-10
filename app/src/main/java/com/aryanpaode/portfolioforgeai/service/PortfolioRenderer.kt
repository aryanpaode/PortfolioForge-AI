package com.aryanpaode.portfolioforgeai.service

import com.aryanpaode.portfolioforgeai.data.model.CareerType
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot

/** Pure renderer: section visibility is independent of skill extraction. No private OCR/evidence is embedded. */
object PortfolioRenderer {
    fun render(snapshot: Snapshot): String {
        val p = requireNotNull(snapshot.profile) { "Complete your profile first." }
        require(p.name.isNotBlank()) { "Add your name first." }
        val groups = snapshot.skills.groupBy { it.category }
        val items = snapshot.items.groupBy { it.type }
        val order = CareerType.values().map { it.label } + items.keys.filter { key -> CareerType.values().none { it.label == key } }
        return buildString {
            appendLine("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            appendLine("<title>${escape(p.name)} — PortfolioForge</title>")
            appendLine("""<style>
                :root{font-family:Arial,system-ui,sans-serif;color:#282020;background:#fff8f8;line-height:1.65}
                *{box-sizing:border-box}body{margin:0}.shell{max-width:1020px;margin:auto;padding:32px 22px 56px}
                .hero{background:#b71c1c;color:white;border-radius:22px;padding:34px 36px;box-shadow:0 12px 30px #61000018}
                .eyebrow{font-size:.8rem;letter-spacing:.17em;text-transform:uppercase;opacity:.86;margin:0 0 8px}
                h1{font-size:clamp(2rem,5vw,3.2rem);line-height:1.14;margin:0 0 12px;overflow-wrap:anywhere}
                .role{font-size:1.2rem;font-weight:700}.summary{font-size:1.05rem;max-width:78ch;white-space:pre-line}
                .links{display:flex;flex-wrap:wrap;gap:9px;margin-top:18px}.links a{background:#fff;color:#981313;text-decoration:none;border-radius:8px;padding:7px 13px;font-weight:700}
                .section{background:#fff;border:1px solid #eed0d0;border-radius:16px;padding:24px 28px;margin-top:22px}
                h2{color:#a81717;font-size:1.5rem;margin:0 0 16px;line-height:1.3}h3{margin:0 0 5px;font-size:1.13rem;line-height:1.45;overflow-wrap:anywhere}
                .skill-group{margin-top:14px}.skill-group h3{font-size:.94rem;color:#665050}.chips{display:flex;flex-wrap:wrap;gap:7px;margin-top:8px}
                .chip{background:#fff2f1;color:#822020;border:1px solid #f0d2cf;padding:5px 11px;border-radius:30px;font-size:.94rem}
                .entry{border-left:3px solid #bd3636;padding:2px 0 2px 18px;margin:22px 0}.entry:last-child{margin-bottom:2px}
                .meta{color:#725959;font-size:.97rem}.description{white-space:pre-line;margin:8px 0;overflow-wrap:anywhere}.entry a{color:#a81717;font-weight:700;overflow-wrap:anywhere}
                footer{color:#796666;text-align:center;margin-top:30px;font-size:.88rem}
                @media(max-width:560px){.shell{padding:16px 12px 35px}.hero{padding:25px 21px}.section{padding:20px 18px}.entry{padding-left:12px}}
                @media print{body{background:white}.shell{padding:0;max-width:none}.hero{box-shadow:none;break-inside:avoid}.section{border:0;padding:15px 0}.entry{break-inside:avoid}a{color:inherit}footer{font-size:10pt}}
            </style></head><body><main class="shell">""")
            appendLine("<header class=\"hero\"><p class=\"eyebrow\">Professional portfolio</p><h1>${escape(p.name)}</h1>")
            if (p.targetRole.isNotBlank()) appendLine("<div class=\"role\">${escape(p.targetRole)}</div>")
            if (p.summary.isNotBlank()) appendLine("<p class=\"summary\">${escape(p.summary)}</p>")
            val academic = listOf(p.branch, p.semester.takeIf { it.isNotBlank() }?.let { "Semester $it" }.orEmpty(), p.cgpa.takeIf { it.isNotBlank() }?.let { "CGPA $it" }.orEmpty()).filter { it.isNotBlank() }
            if (academic.isNotEmpty()) appendLine("<p>${academic.joinToString(" · ") { escape(it) }}</p>")
            val links = link(p.githubUrl, "GitHub") + link(p.linkedInUrl, "LinkedIn") + link(p.portfolioUrl, "Website")
            if (links.isNotBlank()) appendLine("<nav class=\"links\" aria-label=\"Profile links\">$links</nav>")
            appendLine("</header>")
            if (groups.isNotEmpty()) {
                appendLine("<section class=\"section\" data-section=\"Skills\"><h2>Skills</h2>")
                groups.forEach { (category, skills) ->
                    appendLine("<div class=\"skill-group\"><h3>${escape(category)}</h3><div class=\"chips\">${skills.joinToString("") { "<span class=\"chip\">${escape(it.name)}</span>" }}</div></div>")
                }
                appendLine("</section>")
            }
            for (type in order) {
                val entries = items[type].orEmpty()
                if (entries.isEmpty()) continue
                appendLine("<section class=\"section\" data-section=\"${escape(type)}\"><h2>${escape(CareerType.heading(type))}</h2>")
                entries.forEach { item ->
                    appendLine("<article class=\"entry\"><h3>${escape(item.title)}</h3>")
                    val meta = listOf(item.subtitle, item.date).filter { it.isNotBlank() }
                    if (meta.isNotEmpty()) appendLine("<div class=\"meta\">${meta.joinToString(" · ") { escape(it) }}</div>")
                    if (item.description.isNotBlank()) appendLine("<p class=\"description\">${escape(item.description)}</p>")
                    appendLine(link(item.link, when (type) { "Publication" -> "Read publication"; "Certification" -> "View credential"; "Conference" -> "Event / participation link"; else -> "View link" }))
                    appendLine("</article>")
                }
                appendLine("</section>")
            }
            appendLine("<footer>Generated with PortfolioForge AI</footer></main></body></html>")
        }
    }
    fun escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    private fun link(url: String, label: String): String {
        val safe = UrlPolicy.normalize(url) ?: return ""
        return "<a href=\"${escape(safe)}\" target=\"_blank\" rel=\"noopener noreferrer\">${escape(label)}</a>"
    }
}
