package com.github.todd.sortjson

import java.util.regex.Pattern

/**
 * Comparator for natural / alphanumeric string sorting (e.g., "item2" before "item10").
 * Case-insensitive by default with case-sensitive tie-breaker.
 */
object NaturalOrderComparator : Comparator<String> {
    private val CHUNK_PATTERN = Pattern.compile("(\\d+)|(\\D+)")

    override fun compare(s1: String, s2: String): Int {
        val m1 = CHUNK_PATTERN.matcher(s1)
        val m2 = CHUNK_PATTERN.matcher(s2)

        while (m1.find()) {
            if (!m2.find()) return 1

            val chunk1 = m1.group()
            val chunk2 = m2.group()

            val isDigit1 = chunk1[0].isDigit()
            val isDigit2 = chunk2[0].isDigit()

            val result = if (isDigit1 && isDigit2) {
                val num1 = chunk1.toBigIntegerOrNull()
                val num2 = chunk2.toBigIntegerOrNull()
                if (num1 != null && num2 != null) {
                    num1.compareTo(num2)
                } else {
                    chunk1.compareTo(chunk2)
                }
            } else if (isDigit1) {
                -1
            } else if (isDigit2) {
                1
            } else {
                chunk1.compareTo(chunk2, ignoreCase = true)
            }

            if (result != 0) return result
        }

        if (m2.find()) return -1

        // Final tie-breaker: case-sensitive comparison on the whole string
        return s1.compareTo(s2)
    }
}

/**
 * Universal Engine to parse, sort, and serialize:
 * - JSON and JSON5 / JSONC
 * - TypeScript Interfaces (`interface Foo { ... }`)
 * - TypeScript Types (`type Foo = { ... }`)
 * - TypeScript Enums (`enum Foo { ... }`)
 * - JavaScript & TypeScript Object Literals
 * - PHP Associative Arrays (`[ ... ]` or `array( ... )`)
 * - Python Dictionaries (`{ ... }`)
 * - Bare property / type lists (selected lines without outer braces)
 */
class JsonSortEngine(
    private val recursive: Boolean = false,
    private val comparator: Comparator<String> = NaturalOrderComparator
) {

    data class ParsedItem(
        var leadingTrivia: String,
        var rawKey: String,
        val cleanKey: String,
        val spacingBeforeDelimiter: String,
        val delimiter: String,
        val spacingAfterDelimiter: String,
        var value: String,
        var separator: String,
        val trailingTrivia: String,
        val isSpread: Boolean = false
    )

    /**
     * Sorts the given structured text.
     */
    fun sort(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return input

        return try {
            sortInternal(input)
        } catch (_: Exception) {
            // If custom parsing fails on complex syntax, return original input safely
            input
        }
    }

    private fun sortInternal(input: String): String {
        val container = findContainer(input)

        val prefix: String
        val suffix: String
        val body: String
        val isArray: Boolean

        if (container != null) {
            prefix = input.substring(0, container.openIndex + 1)
            suffix = input.substring(container.closeIndex)
            body = input.substring(container.openIndex + 1, container.closeIndex)
            isArray = (container.openChar == '[')
        } else {
            prefix = ""
            suffix = ""
            body = input
            isArray = false
        }

        val (items, closingTrivia) = parseBlockItems(body)
        if (items.isEmpty()) return input

        // Recursively sort values and rawKeys if needed
        if (recursive) {
            for (item in items) {
                val toSort = if (item.value.isNotBlank()) item.value else item.rawKey
                if (toSort.isNotBlank() && (toSort.contains('{') || toSort.contains('['))) {
                    val sortedSub = sort(toSort)
                    if (item.value.isNotBlank()) {
                        item.value = sortedSub
                    } else {
                        item.rawKey = sortedSub
                    }
                }
            }
        }

        val isMultiline = body.contains('\n')
        val sortedBody = serializeItems(items, closingTrivia, isMultiline, isArray)
        return "$prefix$sortedBody$suffix"
    }

    private data class ContainerSpan(val openIndex: Int, val closeIndex: Int, val openChar: Char, val closeChar: Char)

    private fun findContainer(input: String): ContainerSpan? {
        var i = 0
        val len = input.length

        var inSingleQuote = false
        var inDoubleQuote = false
        var inBacktick = false
        var inLineComment = false
        var inBlockComment = false

        var candidateOpen = -1
        var openChar = ' '
        var closeChar = ' '

        while (i < len) {
            val c = input[i]

            // Handle comments
            if (inLineComment) {
                if (c == '\n') inLineComment = false
                i++
                continue
            }
            if (inBlockComment) {
                if (c == '*' && i + 1 < len && input[i + 1] == '/') {
                    inBlockComment = false
                    i += 2
                    continue
                }
                i++
                continue
            }

            // Handle strings
            if (inSingleQuote) {
                if (c == '\\') { i += 2; continue }
                if (c == '\'') inSingleQuote = false
                i++
                continue
            }
            if (inDoubleQuote) {
                if (c == '\\') { i += 2; continue }
                if (c == '"') inDoubleQuote = false
                i++
                continue
            }
            if (inBacktick) {
                if (c == '\\') { i += 2; continue }
                if (c == '`') inBacktick = false
                i++
                continue
            }

            if (c == '/' && i + 1 < len) {
                val next = input[i + 1]
                if (next == '/') { inLineComment = true; i += 2; continue }
                if (next == '*') { inBlockComment = true; i += 2; continue }
            }

            if (c == '\'') { inSingleQuote = true; i++; continue }
            if (c == '"') { inDoubleQuote = true; i++; continue }
            if (c == '`') { inBacktick = true; i++; continue }

            if (c == '{' || c == '[') {
                candidateOpen = i
                openChar = c
                closeChar = if (c == '{') '}' else ']'
                break
            } else if (c == '(' && i >= 5 && input.substring(0, i).trimEnd().endsWith("array")) {
                candidateOpen = i
                openChar = '('
                closeChar = ')'
                break
            }

            i++
        }

        if (candidateOpen == -1) return null

        var depth = 1
        var j = candidateOpen + 1
        inSingleQuote = false
        inDoubleQuote = false
        inBacktick = false
        inLineComment = false
        inBlockComment = false

        while (j < len && depth > 0) {
            val c = input[j]

            if (inLineComment) {
                if (c == '\n') inLineComment = false
                j++
                continue
            }
            if (inBlockComment) {
                if (c == '*' && j + 1 < len && input[j + 1] == '/') {
                    inBlockComment = false
                    j += 2
                    continue
                }
                j++
                continue
            }

            if (inSingleQuote) {
                if (c == '\\') { j += 2; continue }
                if (c == '\'') inSingleQuote = false
                j++
                continue
            }
            if (inDoubleQuote) {
                if (c == '\\') { j += 2; continue }
                if (c == '"') inDoubleQuote = false
                j++
                continue
            }
            if (inBacktick) {
                if (c == '\\') { j += 2; continue }
                if (c == '`') inBacktick = false
                j++
                continue
            }

            if (c == '/' && j + 1 < len) {
                val next = input[j + 1]
                if (next == '/') { inLineComment = true; j += 2; continue }
                if (next == '*') { inBlockComment = true; j += 2; continue }
            }

            if (c == '\'') { inSingleQuote = true; j++; continue }
            if (c == '"') { inDoubleQuote = true; j++; continue }
            if (c == '`') { inBacktick = true; j++; continue }

            if (c == openChar) {
                depth++
            } else if (c == closeChar) {
                depth--
                if (depth == 0) {
                    val rest = input.substring(j + 1).trim()
                    if (rest.isEmpty() || rest == ";" || rest == ",") {
                        return ContainerSpan(candidateOpen, j, openChar, closeChar)
                    }
                }
            }

            j++
        }

        return null
    }

    private fun parseBlockItems(content: String): Pair<List<ParsedItem>, String> {
        val items = mutableListOf<ParsedItem>()
        var pos = 0
        val len = content.length
        var closingTrivia = ""

        while (pos < len) {
            // 1. Read leading trivia (whitespace, newlines, comments)
            val triviaStart = pos
            while (pos < len) {
                val ch = content[pos]
                if (ch.isWhitespace()) {
                    pos++
                } else if (ch == '/' && pos + 1 < len && (content[pos + 1] == '/' || content[pos + 1] == '*')) {
                    if (content[pos + 1] == '/') {
                        while (pos < len && content[pos] != '\n') pos++
                    } else {
                        pos += 2
                        while (pos < len && !(content[pos] == '*' && pos + 1 < len && content[pos + 1] == '/')) {
                            pos++
                        }
                        if (pos < len) pos += 2
                    }
                } else if (ch == '#' && (pos == 0 || content[pos - 1] == '\n' || content[pos - 1] == ' ' || content[pos - 1] == '\t')) {
                    while (pos < len && content[pos] != '\n') pos++
                } else {
                    break
                }
            }
            val leadingTrivia = content.substring(triviaStart, pos)

            if (pos >= len) {
                closingTrivia = leadingTrivia
                break
            }

            // 2. Check for spread property e.g. `...spread`
            if (content.startsWith("...", pos)) {
                val spreadStart = pos
                while (pos < len && content[pos] != ',' && content[pos] != ';' && content[pos] != '\n') {
                    pos++
                }
                val rawExpr = content.substring(spreadStart, pos).trim()
                var sep = ""
                if (pos < len && (content[pos] == ',' || content[pos] == ';')) {
                    sep = content[pos].toString()
                    pos++
                }
                val trailingTrivia = readTrailingComment(content, pos)
                pos += trailingTrivia.length

                items.add(
                    ParsedItem(
                        leadingTrivia = leadingTrivia,
                        rawKey = rawExpr,
                        cleanKey = rawExpr,
                        spacingBeforeDelimiter = "",
                        delimiter = "",
                        spacingAfterDelimiter = "",
                        value = "",
                        separator = sep,
                        trailingTrivia = trailingTrivia,
                        isSpread = true
                    )
                )
                continue
            }

            // 3. Parse Key and Delimiter
            val keyStart = pos
            var bracketDepth = 0
            var braceDepth = 0
            var parenDepth = 0
            var rawKey = ""
            var spacingBeforeDelimiter = ""
            var delimiter = ""

            while (pos < len) {
                val ch = content[pos]

                // Strings
                if (ch == '"' || ch == '\'' || ch == '`') {
                    val q = ch
                    pos++
                    while (pos < len) {
                        val c = content[pos]
                        if (c == '\\') {
                            pos += 2
                        } else if (c == q) {
                            pos++
                            break
                        } else {
                            pos++
                        }
                    }
                    continue
                }

                if (ch == '[') { bracketDepth++; pos++; continue }
                if (ch == ']') { bracketDepth = maxOf(0, bracketDepth - 1); pos++; continue }
                if (ch == '{') { braceDepth++; pos++; continue }
                if (ch == '}') { braceDepth = maxOf(0, braceDepth - 1); pos++; continue }

                if (ch == '(') {
                    if (bracketDepth == 0 && braceDepth == 0 && parenDepth == 0) {
                        // Method signature e.g. `calculate(amount: number): number;`
                        val keySub = content.substring(keyStart, pos)
                        val trimmedKey = keySub.trimEnd()
                        spacingBeforeDelimiter = keySub.substring(trimmedKey.length)
                        rawKey = trimmedKey.trimStart()
                        delimiter = ""
                        break
                    }
                    parenDepth++
                    pos++
                    continue
                }
                if (ch == ')') { parenDepth = maxOf(0, parenDepth - 1); pos++; continue }

                // At depth 0
                if (bracketDepth == 0 && braceDepth == 0 && parenDepth == 0) {
                    if (ch == ':' && (pos + 1 >= len || content[pos + 1] != ':')) {
                        val keySub = content.substring(keyStart, pos)
                        val trimmedKey = keySub.trimEnd()
                        spacingBeforeDelimiter = keySub.substring(trimmedKey.length)
                        rawKey = trimmedKey.trimStart()
                        delimiter = ":"
                        pos++
                        break
                    } else if (ch == '=' && pos + 1 < len && content[pos + 1] == '>') {
                        val keySub = content.substring(keyStart, pos)
                        val trimmedKey = keySub.trimEnd()
                        spacingBeforeDelimiter = keySub.substring(trimmedKey.length)
                        rawKey = trimmedKey.trimStart()
                        delimiter = "=>"
                        pos += 2
                        break
                    } else if (ch == '=' && (pos + 1 >= len || (content[pos + 1] != '>' && content[pos + 1] != '='))) {
                        val keySub = content.substring(keyStart, pos)
                        val trimmedKey = keySub.trimEnd()
                        spacingBeforeDelimiter = keySub.substring(trimmedKey.length)
                        rawKey = trimmedKey.trimStart()
                        delimiter = "="
                        pos++
                        break
                    } else if (ch == ',' || ch == ';' || ch == '\n') {
                        val keySub = content.substring(keyStart, pos)
                        val trimmedKey = keySub.trimEnd()
                        spacingBeforeDelimiter = ""
                        rawKey = trimmedKey.trimStart()
                        delimiter = ""
                        break
                    }
                }

                pos++
            }

            // 4. Spacing after delimiter
            val spacingStart = pos
            while (pos < len && (content[pos] == ' ' || content[pos] == '\t')) pos++
            val spacingAfterDelimiter = content.substring(spacingStart, pos)

            // 5. Read Value until Separator
            val valStart = pos
            bracketDepth = 0
            braceDepth = 0
            parenDepth = 0
            var sep = ""
            var trailingTrivia = ""

            while (pos < len) {
                val ch = content[pos]

                // Strings
                if (ch == '"' || ch == '\'' || ch == '`') {
                    val q = ch
                    pos++
                    while (pos < len) {
                        val c = content[pos]
                        if (c == '\\') {
                            pos += 2
                        } else if (c == q) {
                            pos++
                            break
                        } else {
                            pos++
                        }
                    }
                    continue
                }

                if (ch == '[') { bracketDepth++; pos++; continue }
                if (ch == ']') { bracketDepth = maxOf(0, bracketDepth - 1); pos++; continue }
                if (ch == '{') { braceDepth++; pos++; continue }
                if (ch == '}') { braceDepth = maxOf(0, braceDepth - 1); pos++; continue }
                if (ch == '(') { parenDepth++; pos++; continue }
                if (ch == ')') { parenDepth = maxOf(0, parenDepth - 1); pos++; continue }

                if (bracketDepth == 0 && braceDepth == 0 && parenDepth == 0) {
                    if (ch == ',' || ch == ';') {
                        sep = ch.toString()
                        pos++
                        // Read only trailing inline comment on same line
                        trailingTrivia = readTrailingComment(content, pos)
                        pos += trailingTrivia.length
                        break
                    } else if (ch == '\n') {
                        sep = ""
                        trailingTrivia = ""
                        break
                    }
                }

                pos++
            }

            val fullVal = content.substring(valStart, if (sep.isNotEmpty()) pos - sep.length - trailingTrivia.length else pos)
            val trimmedVal = fullVal.trimEnd()
            if (sep.isEmpty() && closingTrivia.isEmpty()) {
                val trailingWs = fullVal.substring(trimmedVal.length)
                if (trailingWs.isNotEmpty()) {
                    closingTrivia = trailingWs
                }
            }

            items.add(
                ParsedItem(
                    leadingTrivia = leadingTrivia,
                    rawKey = rawKey,
                    cleanKey = extractCleanKey(rawKey),
                    spacingBeforeDelimiter = spacingBeforeDelimiter,
                    delimiter = delimiter,
                    spacingAfterDelimiter = spacingAfterDelimiter,
                    value = trimmedVal,
                    separator = sep,
                    trailingTrivia = trailingTrivia,
                    isSpread = false
                )
            )
        }

        return Pair(items, closingTrivia)
    }

    private fun readTrailingComment(content: String, startPos: Int): String {
        var p = startPos
        val len = content.length
        while (p < len && (content[p] == ' ' || content[p] == '\t')) p++
        if (p < len && ((content[p] == '/' && p + 1 < len && (content[p + 1] == '/' || content[p + 1] == '*')) || content[p] == '#')) {
            val commentStart = startPos
            while (p < len && content[p] != '\n') p++
            return content.substring(commentStart, p)
        }
        return ""
    }

    private fun extractCleanKey(rawKey: String): String {
        var k = rawKey.trim()

        val modifiers = listOf("export", "declare", "readonly", "public", "private", "protected", "static", "override", "abstract", "get", "set", "async")
        var changed = true
        while (changed) {
            changed = false
            for (mod in modifiers) {
                if (k.startsWith("$mod ") || k.startsWith("$mod\t")) {
                    k = k.substring(mod.length).trim()
                    changed = true
                }
            }
        }

        if ((k.startsWith("\"") && k.endsWith("\"")) ||
            (k.startsWith("'") && k.endsWith("'")) ||
            (k.startsWith("`") && k.endsWith("`"))
        ) {
            if (k.length >= 2) k = k.substring(1, k.length - 1)
        }

        if (k.startsWith("[") && k.endsWith("]")) {
            k = k.substring(1, k.length - 1).trim()
            if (k.contains(":")) {
                k = k.substringBefore(":").trim()
            }
        }

        if (k.startsWith("$") && k.length > 1) {
            k = k.substring(1)
        }

        if (k.endsWith("?")) {
            k = k.substring(0, k.length - 1).trim()
        }

        return k
    }

    private fun serializeItems(
        items: List<ParsedItem>,
        closingTrivia: String,
        isMultiline: Boolean,
        isArray: Boolean
    ): String {
        if (items.isEmpty()) return closingTrivia

        val firstHadNewline = items[0].leadingTrivia.contains('\n')
        val firstIndent = items[0].leadingTrivia.replace("\n", "").replace("\r", "")

        val semiCount = items.count { it.separator == ";" }
        val commaCount = items.count { it.separator == "," }
        val defaultSep = if (semiCount >= commaCount && semiCount > 0) ";" else if (commaCount > 0) "," else if (!isMultiline) "," else ""
        val hadTrailingSep = items.last().separator.isNotEmpty()

        val allSorted: List<ParsedItem>
        if (isArray && items.all { it.rawKey.startsWith("{") || it.value.startsWith("{") }) {
            allSorted = items
        } else {
            val leadingSpreads = mutableListOf<ParsedItem>()
            var startIdx = 0
            while (startIdx < items.size && items[startIdx].isSpread) {
                leadingSpreads.add(items[startIdx])
                startIdx++
            }

            val trailingSpreads = mutableListOf<ParsedItem>()
            var endIdx = items.size - 1
            while (endIdx >= startIdx && items[endIdx].isSpread) {
                trailingSpreads.add(0, items[endIdx])
                endIdx--
            }

            val middle = items.subList(startIdx, endIdx + 1).toMutableList()
            middle.sortWith { a, b -> comparator.compare(a.cleanKey, b.cleanKey) }

            val list = ArrayList<ParsedItem>(items.size)
            list.addAll(leadingSpreads)
            list.addAll(middle)
            list.addAll(trailingSpreads)
            allSorted = list
        }

        val sb = StringBuilder()
        for ((idx, it) in allSorted.withIndex()) {
            val isLast = idx == allSorted.size - 1
            val sep = if (!isLast) {
                val s = it.separator.ifEmpty { defaultSep }
                if (!isMultiline && s.isNotEmpty() && !s.endsWith(" ")) "$s " else s
            } else {
                if (!hadTrailingSep) "" else it.separator.ifEmpty { defaultSep }
            }

            var trivia = it.leadingTrivia
            val hasComment = trivia.contains("//") || trivia.contains("/*") || trivia.contains("#")

            if (isMultiline) {
                if (idx == 0) {
                    if (!firstHadNewline && !hasComment) {
                        trivia = trivia.trimStart('\r', '\n')
                        if (trivia.isEmpty()) trivia = firstIndent
                    }
                } else {
                    if (!trivia.startsWith("\n") && !trivia.startsWith("\r\n")) {
                        trivia = "\n$trivia"
                    }
                }
            } else {
                if (idx > 0 && it.separator.isNotEmpty()) {
                    trivia = trivia.trimStart()
                }
            }

            sb.append(trivia)
            sb.append(it.rawKey)
            sb.append(it.spacingBeforeDelimiter)
            sb.append(it.delimiter)
            sb.append(it.spacingAfterDelimiter)
            sb.append(it.value)
            sb.append(sep)
            sb.append(it.trailingTrivia)
        }

        sb.append(closingTrivia)
        return sb.toString()
    }
}
