package com.github.todd.sortjson

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object PsiJsonHelper {
    fun findJsonObject(element: PsiElement?): JsonObject? {
        return PsiTreeUtil.getParentOfType(element, JsonObject::class.java)
    }

    fun findRootJsonObject(file: JsonFile): JsonObject? {
        return file.topLevelValue as? JsonObject
    }
}
