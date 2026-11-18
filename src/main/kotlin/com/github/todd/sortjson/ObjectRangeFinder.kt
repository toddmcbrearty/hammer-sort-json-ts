package com.github.todd.sortjson

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object ObjectRangeFinder {

    /**
     * Determines the text range of the object to sort.
     * 1. Returns current selection if non-empty.
     * 2. Checks IntelliJ JSON PSI if inside a JSON file.
     * 3. Falls back to scanning for balanced enclosing braces { ... } or [ ... ] around caret.
     * 4. If caret is on a declaration line before opening brace (e.g. `interface Foo {`),
     *    detects the opening brace on the same line and selects its block.
     */
    fun findTargetRange(editor: Editor, psiFile: PsiFile?): TextRange? {
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            if (start < end) {
                return TextRange(start, end)
            }
        }

        val caretOffset = editor.caretModel.offset

        // Check JSON PSI if applicable
        if (psiFile is JsonFile) {
            val element = psiFile.findElementAt(caretOffset)
            val jsonObject = PsiTreeUtil.getParentOfType(element, JsonObject::class.java)
            if (jsonObject != null) {
                return jsonObject.textRange
            }

            val topLevelObject = psiFile.topLevelValue as? JsonObject
            if (topLevelObject != null) {
                return topLevelObject.textRange
            }
        }

        // Generic / JS / TS / PHP balanced brace & bracket finder
        val text = editor.document.charsSequence
        val enclosing = findEnclosingBracesRange(text, caretOffset)
        if (enclosing != null) return enclosing

        // If caret is on a declaration line before opening brace, check forward on same line
        return findBraceOnSameLine(text, caretOffset)
    }

    /**
     * Finds the range of the smallest enclosing balanced braces '{ ... }' or '[ ... ]' around offset.
     */
    fun findEnclosingBracesRange(text: CharSequence, offset: Int): TextRange? {
        if (text.isEmpty()) return null
        val safeOffset = offset.coerceIn(0, text.length)

        data class OpenParen(val index: Int, val char: Char)
        val stack = mutableListOf<OpenParen>()

        var inSingleQuote = false
        var inDoubleQuote = false
        var inBacktick = false
        var inLineComment = false
        var inBlockComment = false

        var i = 0
        val len = text.length
        var bestRange: TextRange? = null

        while (i < len) {
            val c = text[i]

            // Handle comments
            if (inLineComment) {
                if (c == '\n') inLineComment = false
                i++
                continue
            }
            if (inBlockComment) {
                if (c == '*' && i + 1 < len && text[i + 1] == '/') {
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

            // Check for comment starts
            if (c == '/' && i + 1 < len) {
                val next = text[i + 1]
                if (next == '/') {
                    inLineComment = true
                    i += 2
                    continue
                }
                if (next == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                }
            }
            if (c == '#' && (i == 0 || text[i - 1] == '\n' || text[i - 1].isWhitespace())) {
                inLineComment = true
                i++
                continue
            }

            // Check for string starts
            if (c == '\'') { inSingleQuote = true; i++; continue }
            if (c == '"') { inDoubleQuote = true; i++; continue }
            if (c == '`') { inBacktick = true; i++; continue }

            if (c == '{' || c == '[') {
                stack.add(OpenParen(i, c))
            } else if ((c == '}' || c == ']') && stack.isNotEmpty()) {
                val expectedOpen = if (c == '}') '{' else '['
                // Find matching open from the end of stack
                val matchIdx = stack.indexOfLast { it.char == expectedOpen }
                if (matchIdx != -1) {
                    val openItem = stack.removeAt(matchIdx)
                    val open = openItem.index
                    val close = i + 1
                    // Check if this pair encloses the caret offset
                    if (open <= safeOffset && safeOffset <= close) {
                        if (bestRange == null || (close - open) < bestRange.length) {
                            bestRange = TextRange(open, close)
                        }
                    }
                }
            }

            i++
        }

        return bestRange
    }

    /**
     * If caret is on a declaration line before opening brace (e.g. `interface Foo {`),
     * finds the opening brace on the same line and its matching closing brace.
     */
    private fun findBraceOnSameLine(text: CharSequence, offset: Int): TextRange? {
        if (text.isEmpty()) return null
        val safeOffset = offset.coerceIn(0, text.length)

        // Find end of current line
        var lineEnd = safeOffset
        while (lineEnd < text.length && text[lineEnd] != '\n') {
            lineEnd++
        }

        // Scan from safeOffset to lineEnd for '{' or '['
        var openPos = -1
        for (i in safeOffset until lineEnd) {
            val c = text[i]
            if (c == '{' || c == '[') {
                openPos = i
                break
            }
        }

        if (openPos == -1) return null

        // With openPos inside the block, finding enclosing braces range will find this block
        return findEnclosingBracesRange(text, openPos + 1)
    }
}
