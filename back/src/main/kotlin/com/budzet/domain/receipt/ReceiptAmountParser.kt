package com.budzet.domain.receipt

import org.springframework.stereotype.Component

@Component
class ReceiptAmountParser {
    // Do not guess from the largest number (card numbers, tax, change, etc.).
    fun parse(text: String): Long? {
        val lines = text.lines().map { it.trim() }
        val label = Regex("^(?:총\\s*결제\\s*금액|결제\\s*금액|총\\s*합계|합계\\s*금액|받을\\s*금액|합계|TOTAL)\\s*[:：]?\\s*", RegexOption.IGNORE_CASE)
        val amount = Regex("^[₩￦]?\\s*((?:[1-9]\\d{0,2}(?:,\\d{3})+)|(?:[1-9]\\d*))\\s*원?$")
        val candidates = lines.mapIndexedNotNull { index, line ->
            val match = label.find(line) ?: return@mapIndexedNotNull null
            val rest = line.substring(match.range.last + 1).trim()
            val value = if (rest.isEmpty()) lines.getOrNull(index + 1).orEmpty() else rest
            amount.matchEntire(value)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
                ?.takeIf { it in 1..999_999_999L }
        }.distinct()
        return candidates.singleOrNull()
    }

}
