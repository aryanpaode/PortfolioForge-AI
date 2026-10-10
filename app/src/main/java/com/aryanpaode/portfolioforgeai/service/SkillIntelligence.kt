package com.aryanpaode.portfolioforgeai.service

import com.aryanpaode.portfolioforgeai.data.model.SkillEntity
import java.util.UUID

/** Local, evidence-based alias matching, not a remote LLM and not a proficiency assessment. */
class SkillIntelligence {
    private data class Term(val name: String, val category: String, val aliases: List<String>)
    private val terms = mutableListOf<Term>()
    init {
        fun group(category: String, vararg entries: String) {
            entries.forEach { entry ->
                val parts = entry.split('|')
                terms += Term(parts.first(), category, parts)
            }
        }
        group("Programming", "Kotlin", "Java", "Python", "C++|CPP", "C#|C Sharp", "JavaScript|JS", "TypeScript", "Dart", "Rust", "Swift", "MATLAB", "R Programming|R language", "C Programming|C language", "Go Programming|Golang", "Bash|Bash scripting", "PHP", "Scala")
        group("Mobile Development", "Android Studio", "Jetpack Compose|Android Compose", "Android", "Flutter", "React Native", "Firebase", "SwiftUI")
        group("Web Development", "HTML|HTML5", "CSS|CSS3", "React|React.js|ReactJS", "Node.js|NodeJS|Node JS", "Express.js|ExpressJS", "Flask", "Django", "FastAPI", "Angular", "Vue.js|VueJS", "REST APIs|RESTful APIs|REST API", "Bootstrap", "Next.js")
        group("Database", "SQL|Structured Query Language", "SQLite", "Room Database|Android Room|Room persistence", "MySQL", "PostgreSQL|Postgres", "MongoDB", "Firestore|Cloud Firestore", "Redis", "Database Design", "Oracle Database")
        group("AI / ML", "Artificial Intelligence|AI", "Machine Learning|ML", "Deep Learning", "CNN|Convolutional Neural Network|Convolutional Neural Networks", "Computer Vision", "TensorFlow", "PyTorch", "Keras", "YOLO|YOLOv5|YOLOv8|YOLOv11", "MediaPipe", "PaddleOCR", "OpenCV", "scikit-learn|sklearn", "Natural Language Processing|NLP", "Retrieval-Augmented Generation (RAG)|Retrieval Augmented Generation|Retrieval-Augmented Generation|RAG", "Generative AI|GenAI|Generative Artificial Intelligence", "Large Language Models (LLMs)|Large Language Model|Large Language Models|LLM|LLMs", "Prompt Engineering", "LangChain", "LlamaIndex", "Hugging Face|HuggingFace", "Transformers", "Vector Databases|Vector Database", "Embeddings|Vector Embeddings", "Semantic Search", "FAISS", "ChromaDB|Chroma DB", "Pinecone", "Fine-tuning|Fine Tuning", "Reinforcement Learning", "OCR|Optical Character Recognition", "BERT")
        group("Data / Analytics", "Data Analysis|Data Analytics", "Data Visualization|Data Visualisation", "Statistics|Statistical Analysis", "Pandas", "NumPy", "Matplotlib", "Seaborn", "Power BI|PowerBI", "Tableau", "Microsoft Excel|MS Excel|Excel", "Apache Spark|PySpark", "Data Engineering", "ETL")
        group("Cloud / DevOps", "AWS|Amazon Web Services", "Amazon EC2|EC2", "Amazon S3|AWS S3", "Microsoft Azure|Azure", "Google Cloud|GCP|Google Cloud Platform", "Docker", "Kubernetes", "Git", "GitHub", "GitLab", "CI/CD|Continuous Integration", "Linux", "Firebase Hosting", "Jenkins", "Terraform")
        group("Networking", "Computer Networking|Networking|Computer Networks", "Cisco Packet Tracer|Packet Tracer", "TCP/IP|TCP IP", "OSPF", "RIP Routing|Routing Information Protocol", "DHCP", "DNS", "VLAN|VLANs|Virtual LAN", "VPN|VPNs|Virtual Private Network", "Windows Server", "NAS|Network Attached Storage", "Routing", "Switching", "Network Security", "Wireshark", "CCNA", "LAN", "WAN")
        group("Security", "Cybersecurity|Cyber Security", "Cryptography", "Penetration Testing", "Ethical Hacking", "Information Security", "Authentication", "OAuth|OAuth2")
        group("Engineering / Research", "Internet of Things|IoT", "ESP32", "Raspberry Pi", "Arduino", "Embedded Systems", "Signal Processing", "Image Processing", "Research Methodology", "Technical Writing", "Scientific Writing", "LaTeX", "VLSI", "Robotics", "CAD", "AutoCAD")
        group("Professional Skills", "Problem Solving|Problem-Solving", "Communication|Communication Skills", "Leadership", "Teamwork|Team Work", "Critical Thinking", "Project Management", "Agile", "Scrum", "Presentation Skills")
    }

    private fun pattern(alias: String): Regex {
        val body = alias.split(Regex("[\\s-]+")).joinToString("[\\s\\-–—]+") { Regex.escape(it) }
        return Regex("(?i)(?<![\\p{L}\\p{N}+#])$body(?![\\p{L}\\p{N}+#])")
    }

    fun normalizeName(value: String): String {
        val clean = value.trim().replace(Regex("\\s+"), " ")
        return terms.firstOrNull { term -> term.aliases.any { pattern(it).matches(clean) } }?.name ?: clean
    }
    fun categorize(name: String): String = terms.firstOrNull { it.name == normalizeName(name) }?.category ?: "Other"

    fun extract(text: String, source: String): List<SkillEntity> {
        // Never scan the filename, OCR engine name, prompts or app metadata for skill evidence.
        val searchable = text.replace(Regex("(?i)https?://\\S+|www\\.\\S+|[\\w.+-]+@[\\w.-]+"), " ")
        val result = terms.mapNotNull { term ->
            val match = term.aliases.firstNotNullOfOrNull { pattern(it).find(searchable) } ?: return@mapNotNull null
            val start = searchable.lastIndexOf('\n', match.range.first).let { if (it < 0) 0 else it + 1 }
            val end = searchable.indexOf('\n', match.range.last).let { if (it < 0) searchable.length else it }
            SkillEntity(UUID.randomUUID().toString(), term.name, term.category, source, searchable.substring(start, end).trim().take(400))
        }.toMutableList()
        // Unlisted technologies in an explicitly labelled skills/tools line are retained for review.
        val labelled = Regex("(?im)^\\s*(?:skills(?:\\s+(?:learned|gained|covered))?|technologies|tools|frameworks|libraries|programming languages)\\s*[:：]\\s*(.+)$")
        labelled.findAll(text).forEach { match ->
            match.groupValues[1].split(Regex(",|;|•|\\s+\\|\\s+")).forEach { raw ->
                val name = normalizeName(raw.trim().trimEnd('.'))
                if (name.length in 2..65 && name.split(' ').size <= 7 && !name.contains("http", true) && !name.contains('@') &&
                    !Regex("(?i)^(?:none|n/a|not applicable|etc)$").matches(name) && result.none { it.name.equals(name, true) }) {
                    result += SkillEntity(UUID.randomUUID().toString(), name, categorize(name), source, match.value.trim().take(400))
                }
            }
        }
        return result.distinctBy { it.name.lowercase() }.sortedWith(compareBy({ it.category }, { it.name }))
    }
}
