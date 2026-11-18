# Hammer Sort Plugin for PhpStorm

A universal structured sorter for PhpStorm and IntelliJ Platform IDEs. Sorts keys and members in **JSON**, **TypeScript interfaces & types**, **JavaScript objects**, **PHP arrays**, **Python dictionaries**, and any structured key-value format.

---

## Description

**Hammer Sort** is a universal structured sorting plugin for PhpStorm and IntelliJ Platform IDEs. It effortlessly organizes and alphabetizes keys, properties, and members across a wide variety of formats—including **JSON**, **TypeScript interfaces & type aliases**, **TypeScript enums**, **JavaScript object literals**, **PHP associative arrays**, **Python dictionaries**, and arbitrary selected lines of code.

Maintaining clean, alphabetically sorted code makes diffs clearer, prevents merge conflicts, and makes large configuration files and type definitions much easier to scan and maintain. Unlike basic line sorters, Hammer Sort is syntax-aware: it preserves comments (JSDoc, block comments, and inline trailing comments), respects TypeScript modifiers (`readonly`, `public`, `?`), handles trailing commas/semicolons seamlessly, and supports both shallow and deep recursive sorting with a single keystroke.

### Quick Keyboard Shortcuts
| Action | macOS | Windows / Linux | Location / Context |
| :--- | :--- | :--- | :--- |
| **Sort Keys** (Shallow) | `Shift + Option + S` | `Shift + Alt + S` | Editor Popup, **Edit** & **Code** Menus |
| **Sort Keys (Recursively)** | `Shift + Option + Ctrl + S` | `Shift + Alt + Ctrl + S` | Editor Popup, **Edit** & **Code** Menus |

---

## Features

- **TypeScript Interfaces & Types**:
  - Full support for `interface Foo { ... }`, `type Foo = { ... }`, and `enum Foo { ... }`.
  - Intelligently ignores modifiers for sorting (`readonly`, `public`, `private`, `protected`, `static`, `export`) while preserving them verbatim in the output.
  - Handles optional properties (`id?: number`), index signatures (`[key: string]: any`), method signatures (`calculate(x: number): void`), and function types.
- **JSON & JSON5 / JSONC**:
  - Full support for standard `.json`, `.json5`, `.jsonc` files and nested data structures.
  - Automatically handles comma placement so the last item never has an invalid trailing comma (unless trailing commas were already present in the source).
- **JavaScript & TypeScript Object Literals**:
  - Unquoted keys, single quotes, double quotes, computed property names (`[computed]`), and shorthand properties.
  - Respects spread properties (`...defaults` stays at the top, `...overrides` stays at the bottom).
- **PHP Associative Arrays**:
  - Modern short array syntax (`[ 'b' => 2, 'a' => 1 ]`) and legacy `array( ... )` syntax.
- **Python Dictionaries**:
  - Standard Python dictionary literals (`{ 'b': 2, 'a': 1 }`).
- **Bare Selection Sorting**:
  - Highlight any subset of lines inside an interface, class, or object without needing wrapping braces — Hammer Sort sorts only the selected properties while keeping comments and indentation intact!
- **Comment & Trivia Preservation**:
  - Leading line comments (`// ...`), JSDoc / block comments (`/** ... */`), and trailing inline comments (`field: string; // inline`) stay attached to their respective fields when sorted.
- **Shallow & Recursive (Deep) Sorting**:
  - **Sort Keys (Shallow)**: Sorts only the immediate keys of the enclosing or selected object/interface.
  - **Sort Keys (Recursively)**: Recursively traverses and sorts all nested objects, nested types, and objects inside arrays.
- **Smart Target Selection**:
  - If text is selected: sorts the selected object, interface, or property lines.
  - If no text is selected: automatically detects the enclosing object, interface, or type around the cursor (or on the declaration line like `interface UserProfile {`).
- **Natural Alphanumeric Sorting**: Sorts `"item2"` before `"item10"`, case-insensitively with case-sensitive tie-breaking.
- **Full Undo / Redo**: Every sort action is a single atomic undo step (`Cmd+Z` / `Ctrl+Z`).

---

## Keyboard Shortcuts & Menus

| Action | Shortcut (macOS) | Shortcut (Win / Linux) | Location / Context |
| :--- | :--- | :--- | :--- |
| **Sort Keys** (Shallow) | `Shift + Option + S` | `Shift + Alt + S` | Editor Popup, **Edit** Menu, **Code** Menu |
| **Sort Keys (Recursively)** | `Shift + Option + Ctrl + S` | `Shift + Alt + Ctrl + S` | Editor Popup, **Edit** Menu, **Code** Menu |

*(Keybindings can be customized in **Settings** → **Keymap** → search "Sort Keys")*

---

## Supported Formats & Examples

### 1. TypeScript Interfaces & Types
**Before:**
```ts
interface UserProfile {
  z: string;
  // Unique DB identifier
  readonly id?: number;
  calculateTax?(amount: number): number;
  avatar: {
    url: string;
    width: number;
    height: number;
  };
  name: string; // display name
}
```

**After Sort Keys:**
```ts
interface UserProfile {
  avatar: {
    url: string;
    width: number;
    height: number;
  };
  calculateTax?(amount: number): number;
  // Unique DB identifier
  readonly id?: number;
  name: string; // display name
  z: string;
}
```

### 2. JSON Objects
**Before:**
```json
{
  "dependencies": {
    "vue": "^3.0.0",
    "axios": "^1.0.0",
    "pinia": "^2.0.0"
  }
}
```

**After Sort Keys (Recursively):**
```json
{
  "dependencies": {
    "axios": "^1.0.0",
    "pinia": "^2.0.0",
    "vue": "^3.0.0"
  }
}
```

### 3. PHP Associative Arrays
**Before:**
```php
$user = [
    'zebra' => 10,
    'apple' => 20,
    'banana' => 30,
];
```

**After Sort Keys:**
```php
$user = [
    'apple' => 20,
    'banana' => 30,
    'zebra' => 10,
];
```

---

## Development & Building

### Build Plugin Distribution (`.zip`)
```bash
./gradlew buildPlugin
```
Generated ZIP location:
```
build/distributions/hammer-sort-json-1.0.1.zip
```

### Run Tests
```bash
./gradlew test
```

### Install into PhpStorm
1. Run `./gradlew buildPlugin`.
2. Open PhpStorm **Settings** (`Cmd + ,`) → **Plugins**.
3. Click ⚙️ (Gear Icon) → **Install Plugin from Disk...**.
4. Choose `build/distributions/hammer-sort-json-1.0.1.zip`.
