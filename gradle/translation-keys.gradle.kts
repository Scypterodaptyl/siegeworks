/**
 * Fails the build when a screen button, a map row or a message names a translation that does not
 * exist in every language file.
 *
 * <p>Keys are plain strings: the compiler is content with a name nobody ever wrote a translation
 * for, and the only thing that noticed was a player reading the raw key off a button. That is a
 * poor last line of defence for something arithmetic can check in a second.
 */
val checkTranslationKeys by tasks.registering {
    group = "verification"
    description = "Checks that every translation key the code uses exists in every language."

    // From the root of the repository: the shared sources live there, while this project is one
    // version's build directory and holds nothing but what stonecutter generates into it.
    val sources = fileTree(rootProject.file("src/main/java")) { include("**/*.java") }
    val languages = fileTree(rootProject.file("src/main/resources/assets/${project.property("mod.id")}/lang")) {
        include("*.json")
    }
    inputs.files(sources, languages)
    outputs.upToDateWhen { false }

    // Held as plain values, because a task that reaches back into the build script cannot be
    // stored in the configuration cache.
    val sourceFiles = sources.files.toList()
    val languageFiles = languages.files.toList()
    val id = project.property("mod.id") as String

    doLast {
        if (languageFiles.isEmpty()) {
            return@doLast
        }
        val declaredPattern = Regex("\"([^\"\\\\]+)\"\\s*:")
        val declared = languageFiles.associate { file ->
            file.name to declaredPattern.findAll(file.readText())
                .map { it.groupValues[1] }
                .toSet()
        }

        val literal = Regex("Component\\.translatable\\(\\s*\"([^\"]+)\"")
        val screenButton = Regex("(?:layout\\.add\\w*|addButton)\\([^;]*?\"([a-z_]+)\"",
            RegexOption.DOT_MATCHES_ALL)
        val mapRow = Regex("row\\w*\\([A-Z_]+,\\s*\"([a-z_]+)\"")

        val used = mutableSetOf<String>()
        sourceFiles.forEach { file ->
            val text = file.readText()
            literal.findAll(text).forEach { used += it.groupValues[1] }
            if (file.name == "RecruitsSiegeCommandCategory.java") {
                screenButton.findAll(text).forEach {
                    used += "gui.$id.recruits.siege_commands.text." + it.groupValues[1]
                    used += "gui.$id.recruits.siege_commands.tooltip." + it.groupValues[1]
                }
            }
            mapRow.findAll(text).forEach {
                used += "gui.$id.rts.action." + it.groupValues[1]
                used += "gui.$id.rts.action." + it.groupValues[1] + ".hint"
            }
        }

        // Only this mod's own keys, and only whole ones: a key joined from pieces at run time turns
        // up here as its bare prefix, which no language file was ever meant to hold.
        val ours = used.filter { key ->
            (key.startsWith("gui.$id.") || key.startsWith("message.$id.") ||
                key.startsWith("siege.")) && !key.endsWith(".")
        }
        val missing = ours.flatMap { key ->
            declared.filterValues { !it.contains(key) }.keys.map { language -> "$key ($language)" }
        }.sorted()
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Translation keys with no translation:" + System.lineSeparator() + "  " +
                    missing.joinToString(System.lineSeparator() + "  ")
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkTranslationKeys)
}

