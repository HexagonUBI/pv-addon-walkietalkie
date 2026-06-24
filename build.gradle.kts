plugins {
    id("java")
    // ModDevGradle — the current NeoForge build plugin for 1.21.1.
    // If Gradle complains, bump this to the latest 2.0.x from the plugin portal.
    id("net.neoforged.moddev") version "2.0.78"
}

// gradle.properties uses snake_case keys, so look them up explicitly
// (a `by project` delegate would search for the camelCase name verbatim and miss).
val modId = property("mod_id") as String
val modVersion = property("mod_version") as String
val modGroup = property("mod_group") as String
val minecraftVersion = property("minecraft_version") as String
val neoVersion = property("neo_version") as String
val pvVersion = property("pv_version") as String

version = modVersion
group = modGroup

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    // Plasmo Voice API + platform sources
    maven("https://repo.plasmoverse.com/releases")
    maven("https://repo.plasmoverse.com/snapshots")
}

neoForge {
    version = neoVersion

    // Generates run configs (client/server/data) under build/.
    runs {
        register("client") { client() }
        register("server") { server() }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    // ---- Plasmo Voice ----
    // compileOnly: the player/server already ships Plasmo Voice; we do NOT bundle it.
    // server  -> ServerActivation, ServerSourceLine, ServerBroadcastSource, etc.
    // client  -> client-side activation / mic-capture bridge.
    //
    // We exclude Guava: PV's API transitively pulls guava 33.3.1-jre, but Minecraft 1.21.1
    // pins guava to "strictly 32.1.2-jre". Those two strict constraints can't both be
    // satisfied, so resolution fails. Minecraft already puts Guava on the classpath, and PV
    // is compileOnly (the real PV mod provides everything at runtime), so dropping PV's copy
    // is safe — we keep PV's own su.plo.voice.* modules (common/proto/proxy) that we compile
    // against, and let Minecraft's Guava win.
    compileOnly("su.plo.voice.api:server:$pvVersion") {
        exclude(group = "com.google.guava")
    }
    compileOnly("su.plo.voice.api:client:$pvVersion") {
        exclude(group = "com.google.guava")
    }

    // Optional: platform module so you can read PV sources / use extensions in your IDE.
    // compileOnly("su.plo.voice:protocol:$pvVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to modId,
        "mod_version" to modVersion,
        "minecraft_version" to minecraftVersion,
        "neo_version" to neoVersion,
        "pv_version" to pvVersion
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}
