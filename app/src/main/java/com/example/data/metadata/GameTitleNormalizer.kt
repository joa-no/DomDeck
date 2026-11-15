package com.example.data.metadata

import java.text.Normalizer

object GameTitleNormalizer {

    private val KNOWN_EXTENSIONS = setOf(
        "chd", "cue", "pbp", "iso", "m3u", "bin", "img", "ccd", "mdf", "mds"
    )

    private val BRACKET_TAG_REGEX = Regex("""\[[^\]]*\]""")
    private val PAREN_TAG_REGEX = Regex("""\([^)]*\)""")
    private val TRAILING_DISC_REGEX = Regex(
        """(?i)(?:\s*[-–—]?\s*(?:disc|cd|disk)\s*\d+(?:\s*(?:of|de|/)\s*\d+)?)\s*$"""
    )
    private val VERSION_OR_REV_TAG_REGEX = Regex(
        """(?i)\((?:disc|cd|disk|rev|v\d|beta|demo|proto|track|edc|no\s*edc|unl|alt|track\s*\d+)[^)]*\)"""
    )
    private val SERIAL_IN_PARENS_REGEX = Regex("""\((?:SLUS|SCUS|SLES|SCES|SLPS|SLPM|SCPS)[^)]*\)""", RegexOption.IGNORE_CASE)
    private val MULTISPACE_REGEX = Regex("""\s+""")

    private val PS1_PLATFORM_REGEX = Regex(
        """(?i)\bPlayStation\b(?!\s*[2345]|\s+Portable|\s+Vita)|\b(PS1|PSX|PS\s?one)\b"""
    )
    private val OTHER_CONSOLE_ONLY_REGEX = Regex(
        """(?i)\b(?:PlayStation\s*[2345]|PS2|PS3|PS4|PS5|PSP|PS\s?Vita|Xbox|Nintendo\s*(?:64|Switch|GameCube|Wii)|Dreamcast|Sega\s*Saturn)\b"""
    )
    private val NON_GAME_HINT_REGEX = Regex(
        """(?i)\b(?:film|filme|movie|soundtrack|trilha\s+sonora|album|álbum|manga|mangá|novel|romance|tv\s+series|série\s+de\s+televisão|comic|quadrinhos|board\s+game)\b"""
    )
    private val VIDEO_GAME_HINT_REGEX = Regex(
        """(?i)\b(?:video\s*game|jogo\s+eletr[oô]nico|survival\s+horror|role-playing|rpg|platformer|action|racing|fighting|shooter|konami|sony|capcom|namco|squaresoft|square|naughty\s+dog|playstation)\b"""
    )

    fun stripKnownExtension(fileName: String): String {
        val trimmed = fileName.trim()
        val lastDot = trimmed.lastIndexOf('.')
        if (lastDot <= 0 || lastDot == trimmed.lastIndex) return trimmed
        val ext = trimmed.substring(lastDot + 1).lowercase()
        return if (ext in KNOWN_EXTENSIONS) trimmed.substring(0, lastDot) else trimmed
    }

    /**
     * Normalizes a ROM filename exclusively for metadata queries without altering
     * the original filename or SAF URI.
     * Example: "Silent Hill (USA).cue" -> "Silent Hill"
     */
    fun normalizeForSearch(fileName: String): String {
        val noExt = stripKnownExtension(fileName).replace('_', ' ')
        val cleaned = noExt
            .replace(BRACKET_TAG_REGEX, " ")
            .replace(PAREN_TAG_REGEX, " ")
            .replace(TRAILING_DISC_REGEX, " ")
            .replace(MULTISPACE_REGEX, " ")
            .trim()
            .trim('-', '–', '—', ':', '.')
            .trim()
        return cleaned.ifBlank { stripKnownExtension(fileName).replace('_', ' ').trim() }
    }

    fun extractRegionHint(fileName: String): String? {
        val matches = Regex("""\(([^)]+)\)""").findAll(fileName)
        for (match in matches) {
            val content = match.groupValues[1].trim()
            when {
                content.contains("USA", ignoreCase = true) -> return "USA"
                content.contains("Europe", ignoreCase = true) -> return "Europe"
                content.contains("Japan", ignoreCase = true) -> return "Japan"
                content.contains("World", ignoreCase = true) -> return "World"
                content.equals("U", ignoreCase = true) -> return "USA"
                content.equals("E", ignoreCase = true) -> return "Europe"
                content.equals("J", ignoreCase = true) -> return "Japan"
            }
        }
        return null
    }

    /**
     * Libretro thumbnails replace & * / : < > ? \ | " with underscores.
     */
    fun sanitizeLibretroName(name: String): String {
        val forbidden = setOf('&', '*', '/', ':', '<', '>', '?', '\\', '|', '"')
        return buildString(name.length) {
            for (ch in name) {
                append(if (ch in forbidden) '_' else ch)
            }
        }.trim()
    }

    /**
     * Generates ordered, unique candidate stems (without .png) for Libretro's
     * "Sony - PlayStation/Named_Boxarts/" directory.
     */
    fun libretroBoxartCandidates(fileName: String): List<String> {
        val rawStem = stripKnownExtension(fileName).replace('_', ' ').trim()
        val cleanTitle = normalizeForSearch(fileName)
        val regionHint = extractRegionHint(fileName)

        val strippedTechnicalTags = rawStem
            .replace(BRACKET_TAG_REGEX, " ")
            .replace(VERSION_OR_REV_TAG_REGEX, " ")
            .replace(SERIAL_IN_PARENS_REGEX, " ")
            .replace(TRAILING_DISC_REGEX, " ")
            .replace(MULTISPACE_REGEX, " ")
            .trim()

        val ordered = linkedSetOf<String>()

        fun addCandidate(candidate: String) {
            val sanitized = sanitizeLibretroName(candidate)
            if (sanitized.isNotBlank()) {
                ordered.add(sanitized)
            }
        }

        if (rawStem.isNotBlank()) addCandidate(rawStem)
        if (strippedTechnicalTags.isNotBlank()) addCandidate(strippedTechnicalTags)

        if (cleanTitle.isNotBlank()) {
            if (regionHint != null) {
                addCandidate("$cleanTitle ($regionHint)")
            }
            listOf("USA", "Europe", "Japan", "World").forEach { region ->
                if (!region.equals(regionHint, ignoreCase = true)) {
                    addCandidate("$cleanTitle ($region)")
                }
            }
            addCandidate(cleanTitle)
        }

        return ordered.toList().take(7)
    }

    fun normalizeComparableTitle(title: String): String {
        val noParens = title.replace(PAREN_TAG_REGEX, " ")
        val decomposed = Normalizer.normalize(noParens, Normalizer.Form.NFD)
            .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
        return decomposed
            .lowercase()
            .replace('&', ' ')
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(MULTISPACE_REGEX, " ")
            .trim()
    }

    /**
     * Ensures Wikimedia results match the exact queried PS1 game and do NOT confuse
     * original releases with sequels ("Silent Hill 2"), remakes, films, or other consoles.
     */
    fun isValidPs1WikipediaMatch(
        queryTitle: String,
        articleTitle: String,
        articleType: String?,
        description: String?,
        extractOrSnippet: String?
    ): Boolean {
        if (articleType.equals("disambiguation", ignoreCase = true)) return false
        if (articleTitle.contains("disambiguation", ignoreCase = true) ||
            articleTitle.contains("desambiguação", ignoreCase = true)
        ) {
            return false
        }

        val combinedMeta = listOfNotNull(articleTitle, description, extractOrSnippet).joinToString(" ")
        if (combinedMeta.isBlank()) return false

        // Reject films, albums, books, comics unless explicitly queried as part of game name
        val queryNormalized = normalizeComparableTitle(queryTitle)
        if (queryNormalized.isBlank()) return false

        val descOrTitle = listOfNotNull(articleTitle, description).joinToString(" ")
        if (NON_GAME_HINT_REGEX.containsMatchIn(descOrTitle) &&
            !NON_GAME_HINT_REGEX.containsMatchIn(queryTitle) &&
            !VIDEO_GAME_HINT_REGEX.containsMatchIn(descOrTitle)
        ) {
            return false
        }

        // Strict title check: prevent sequels / subtitles / remakes when absent from query
        val articleNormalized = normalizeComparableTitle(articleTitle)
        val normalizedMatches = articleNormalized == queryNormalized ||
            articleNormalized.removePrefix("the ").trim() == queryNormalized.removePrefix("the ").trim()

        if (!normalizedMatches) {
            return false
        }

        // Reject articles labeled explicitly in parentheses as film/remake/etc.
        val parenMatches = Regex("""\(([^)]+)\)""").findAll(articleTitle).map { it.groupValues[1] }.toList()
        if (parenMatches.any { NON_GAME_HINT_REGEX.containsMatchIn(it) }) {
            return false
        }
        if (parenMatches.any { it.contains("2024", ignoreCase = true) || it.contains("remake", ignoreCase = true) } &&
            !queryTitle.contains("remake", ignoreCase = true)
        ) {
            return false
        }

        // Verify PlayStation 1 platform explicitly
        val mentionsPs1 = PS1_PLATFORM_REGEX.containsMatchIn(combinedMeta)
        if (!mentionsPs1) {
            return false
        }

        // Ensure it looks like a video game article (not an unrelated historical page mentioning PlayStation)
        val hasGameSignal = VIDEO_GAME_HINT_REGEX.containsMatchIn(combinedMeta)
        if (!hasGameSignal) {
            return false
        }

        // If the short description itself is explicitly for another console only (e.g., "PlayStation 2 video game")
        // without mentioning original PlayStation, reject it.
        if (!description.isNullOrBlank() &&
            OTHER_CONSOLE_ONLY_REGEX.containsMatchIn(description) &&
            !PS1_PLATFORM_REGEX.containsMatchIn(description)
        ) {
            return false
        }

        return true
    }
}
