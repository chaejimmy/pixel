package com.shourov.apps.pacedream.feature.inbox.data

/**
 * Lightweight local content moderation pre-check.
 *
 * Uses word-boundary regex matching (aligned with backend and iOS) to avoid
 * substring false positives (e.g. "hello" must NOT match "hell").
 *
 * This is a client-side convenience check for instant UX feedback.
 * The backend `/v1/moderation/check` remains the source of truth.
 */
object ContentModerationCheck {

    enum class Status { ALLOW, WARN, BLOCK }

    data class Result(
        val status: Status,
        val categories: List<String>,
        val message: String
    )

    private data class PatternEntry(
        val regex: Regex,
        val category: String
    )

    private fun wordBoundary(phrase: String): PatternEntry {
        val escaped = Regex.escape(phrase)
        return PatternEntry(
            regex = Regex("\\b$escaped\\b", RegexOption.IGNORE_CASE),
            category = ""
        )
    }

    private fun entry(phrase: String, category: String): PatternEntry {
        val escaped = Regex.escape(phrase)
        return PatternEntry(
            regex = Regex("\\b$escaped\\b", RegexOption.IGNORE_CASE),
            category = category
        )
    }

    // Block patterns — aligned with backend BLOCK_ENTRIES
    private val blockPatterns: List<PatternEntry> = listOf(
        // Threats / violence
        entry("i will kill", "threat"), entry("i'm going to kill", "threat"),
        entry("i'll kill", "threat"), entry("death threat", "threat"),
        entry("i will hurt you", "threat"), entry("i'm gonna hurt you", "threat"),
        // Explicit sexual solicitation
        entry("sex for money", "sexual"), entry("looking for sex", "sexual"),
        entry("escort service", "sexual"), entry("sexual favors", "sexual"),
        entry("send nudes", "sexual"), entry("nude pics", "sexual"),
        // Hate speech / slurs
        entry("nigger", "hate"), entry("faggot", "hate"),
        entry("kike", "hate"), entry("chink", "hate"),
        entry("spic", "hate"), entry("tranny", "hate"),
        // Scam
        entry("send me your password", "scam"), entry("wire transfer", "scam"),
        entry("western union", "scam"), entry("money gram", "scam"),
        entry("gift card payment", "scam"), entry("send bitcoin", "scam"),
        entry("crypto payment only", "scam"),
        // Prohibited
        entry("escort service", "prohibited"), entry("brothel", "prohibited"),
        entry("gun sale", "prohibited"), entry("stolen goods", "prohibited"),
        entry("counterfeit", "prohibited"), entry("fake id", "prohibited"),
        entry("drug deal", "prohibited"), entry("human trafficking", "prohibited"),
    )

    // Warn patterns — aligned with backend WARN_ENTRIES
    private val warnPatterns: List<PatternEntry> = listOf(
        // Profanity
        entry("shit", "profanity"), entry("fuck", "profanity"),
        entry("bitch", "profanity"), entry("dick", "profanity"),
        entry("bastard", "profanity"), entry("damn", "profanity"),
        entry("hell", "profanity"), entry("crap", "profanity"),
        entry("wtf", "profanity"), entry("stfu", "profanity"),
        entry("ass", "profanity"), entry("piss", "profanity"),
        // Harassment
        entry("you're stupid", "harassment"), entry("you're an idiot", "harassment"),
        entry("loser", "harassment"), entry("kill yourself", "harassment"),
        entry("kys", "harassment"),
        // Spam
        entry("click here", "spam"), entry("free money", "spam"),
        entry("act now", "spam"), entry("limited time offer", "spam"),
        entry("buy now", "spam"), entry("make money fast", "spam"),
        // Suspicious off-platform
        entry("cash only", "suspicious"), entry("no id needed", "suspicious"),
        entry("off platform", "suspicious"), entry("telegram only", "suspicious"),
        entry("whatsapp only", "suspicious"), entry("no questions asked", "suspicious"),
        entry("meet me outside", "suspicious"),
    )

    /**
     * Check text for moderation issues using word-boundary matching.
     * Returns [Result] with status, matched categories, and user-facing message.
     */
    fun check(text: String): Result {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result(Status.ALLOW, emptyList(), "")
        }

        // 1. Block checks
        for (pattern in blockPatterns) {
            if (pattern.regex.containsMatchIn(trimmed)) {
                return Result(
                    status = Status.BLOCK,
                    categories = listOf(pattern.category),
                    message = "This content violates our community guidelines and cannot be submitted."
                )
            }
        }

        // 2. Warn checks
        val warnCategories = mutableSetOf<String>()
        for (pattern in warnPatterns) {
            if (pattern.regex.containsMatchIn(trimmed)) {
                warnCategories.add(pattern.category)
            }
        }

        if (warnCategories.isNotEmpty()) {
            return Result(
                status = Status.WARN,
                categories = warnCategories.sorted(),
                message = "Your message may contain content that goes against our community guidelines. Please review and edit before submitting."
            )
        }

        return Result(Status.ALLOW, emptyList(), "")
    }
}
