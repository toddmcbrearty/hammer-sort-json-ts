package com.github.todd.sortjson

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EngineTest {

    @Test
    fun testNaturalOrderComparator() {
        val list = listOf("item10", "item2", "item1", "Item3", "ITEM20")
        val sorted = list.sortedWith(NaturalOrderComparator)
        assertEquals(listOf("item1", "item2", "Item3", "item10", "ITEM20"), sorted)
    }

    @Test
    fun testBasicJsonShallowSort() {
        val input = """
            {
              "banana": 1,
              "apple": 2,
              "carrot": 3
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            {
              "apple": 2,
              "banana": 1,
              "carrot": 3
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testShallowSortDoesNotSortNested() {
        val input = """
            {
              "z": 1,
              "a": {
                "d": 4,
                "c": 3
              }
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            {
              "a": {
                "d": 4,
                "c": 3
              },
              "z": 1
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testRecursiveSortSortsNestedObjects() {
        val input = """
            {
              "z": 1,
              "a": {
                "d": 4,
                "c": 3
              }
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = true)
        val sorted = engine.sort(input)

        val expected = """
            {
              "a": {
                "c": 3,
                "d": 4
              },
              "z": 1
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testJsObjectLiteralWithUnquotedKeys() {
        val input = """
            {
              zebra: 10,
              apple: "red",
              banana: true
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            {
              apple: "red",
              banana: true,
              zebra: 10
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testJsObjectLiteralWithSingleQuotes() {
        val input = """
            {
              'zebra': 10,
              'apple': 'fruit'
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            {
              'apple': 'fruit',
              'zebra': 10
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testRecursiveSortInsideArray() {
        val input = """
            {
              items: [
                { z: 1, a: 2 },
                { y: 3, b: 4 }
              ],
              alpha: "first"
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = true)
        val sorted = engine.sort(input)

        assertTrue(sorted.indexOf("alpha:") < sorted.indexOf("items:"))
        assertTrue(sorted.contains("a: 2") && sorted.contains("z: 1"))
        assertTrue(sorted.indexOf("a: 2") < sorted.indexOf("z: 1"))
        assertTrue(sorted.indexOf("b: 4") < sorted.indexOf("y: 3"))
    }

    @Test
    fun testSingleLineObject() {
        val input = "{ \"b\": 2, \"a\": 1 }"
        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)
        assertEquals("{ \"a\": 1, \"b\": 2 }", sorted)
    }

    @Test
    fun testTrailingCommaPreserved() {
        val input = """
            {
              b: 2,
              a: 1,
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            {
              a: 1,
              b: 2,
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptInterfaceBasic() {
        val input = """
            interface UserProfile {
              z: string;
              a: number;
              b?: boolean;
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            interface UserProfile {
              a: number;
              b?: boolean;
              z: string;
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptInterfaceWithModifiersAndOptional() {
        val input = """
            interface AdminUser {
              readonly id: number;
              public name: string;
              bio?: string;
              avatar: string;
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            interface AdminUser {
              avatar: string;
              bio?: string;
              readonly id: number;
              public name: string;
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptInterfaceWithComments() {
        val input = """
            interface Config {
              // Timeout in ms
              timeout: number;
              /**
               * Cache settings
               */
              cache: boolean;
              apiKey: string; // Secret key
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            interface Config {
              apiKey: string; // Secret key
              /**
               * Cache settings
               */
              cache: boolean;
              // Timeout in ms
              timeout: number;
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptInterfaceWithMethods() {
        val input = """
            interface ApiClient {
              logout(): Promise<void>;
              login(credentials: Credentials): Promise<User>;
              checkStatus(): boolean;
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            interface ApiClient {
              checkStatus(): boolean;
              login(credentials: Credentials): Promise<User>;
              logout(): Promise<void>;
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptTypeNestedRecursive() {
        val input = """
            type ResponseData = {
              user: {
                zip: number;
                city: string;
              };
              status: number;
            };
        """.trimIndent()

        val engine = JsonSortEngine(recursive = true)
        val sorted = engine.sort(input)

        val expected = """
            type ResponseData = {
              status: number;
              user: {
                city: string;
                zip: number;
              };
            };
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptEnum() {
        val input = """
            enum Direction {
              Up = "UP",
              Down = "DOWN",
              Right = "RIGHT",
              Left = "LEFT",
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            enum Direction {
              Down = "DOWN",
              Left = "LEFT",
              Right = "RIGHT",
              Up = "UP",
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testTypeScriptSpreadOperators() {
        val input = """
            const merged = {
              ...defaults,
              z: 10,
              a: 20,
              ...overrides,
            };
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            const merged = {
              ...defaults,
              a: 20,
              z: 10,
              ...overrides,
            };
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testPhpAssociativeArray() {
        val input = """
            ${'$'}data = [
                'zebra' => 10,
                'apple' => 20,
                'banana' => 30,
            ];
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            ${'$'}data = [
                'apple' => 20,
                'banana' => 30,
                'zebra' => 10,
            ];
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testPhpOldArraySyntax() {
        val input = """
            array(
                'z' => 1,
                'a' => 2,
            )
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            array(
                'a' => 2,
                'z' => 1,
            )
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testPythonDictionary() {
        val input = """
            {
                'z': 1,
                'a': 2,
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            {
                'a': 2,
                'z': 1,
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testBareSelectedLinesWithoutBraces() {
        val input = """
              zeta: string;
              beta: number;
              alpha: boolean;
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
              alpha: boolean;
              beta: number;
              zeta: string;
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testIndexSignatures() {
        val input = """
            interface MapObj {
              name: string;
              [key: string]: any;
              age: number;
            }
        """.trimIndent()

        val engine = JsonSortEngine(recursive = false)
        val sorted = engine.sort(input)

        val expected = """
            interface MapObj {
              age: number;
              [key: string]: any;
              name: string;
            }
        """.trimIndent()

        assertEquals(expected, sorted)
    }

    @Test
    fun testFindEnclosingBracesSimple() {
        val text = "const x = { b: 1, a: 2 };"
        val caret = text.indexOf("b:")
        val range = ObjectRangeFinder.findEnclosingBracesRange(text, caret)
        assertNotNull(range)
        assertEquals("{ b: 1, a: 2 }", range.substring(text))
    }

    @Test
    fun testFindEnclosingBracesNested() {
        val text = """
            const config = {
              outer: {
                innerB: 1,
                innerA: 2
              }
            };
        """.trimIndent()

        val innerCaret = text.indexOf("innerB")
        val innerRange = ObjectRangeFinder.findEnclosingBracesRange(text, innerCaret)
        assertNotNull(innerRange)
        assertTrue(innerRange.substring(text).contains("innerB: 1"))
        assertFalse(innerRange.substring(text).contains("outer:"))

        val outerCaret = text.indexOf("outer:")
        val outerRange = ObjectRangeFinder.findEnclosingBracesRange(text, outerCaret)
        assertNotNull(outerRange)
        assertTrue(outerRange.substring(text).contains("outer:"))
    }
}
