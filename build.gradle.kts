plugins {
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    implementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")

    // Meteor
    implementation("meteordevelopment:meteor-client:${properties["meteor_version"] as String}")

    // Optional map and pathfinding integrations; supplied by the user's installed mods.
    compileOnly("maven.modrinth:xaeroplus:2.37.2+fabric-26.3") { isTransitive = false }
    compileOnly("maven.modrinth:xaeros-world-map:fabric-26.3-1.47.2") { isTransitive = false }
    compileOnly("maven.modrinth:xaeros-minimap:fabric-26.3-26.6.2") { isTransitive = false }
    compileOnly("meteordevelopment:baritone:26.3-SNAPSHOT") { isTransitive = false }
    compileOnly("net.lenni0451:LambdaEvents:2.4.2")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of((properties["java_version"] as String).toInt())
}

tasks {
    test { useJUnitPlatform() }
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
            "java_version" to project.property("java_version"),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        val licenseSuffix = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${licenseSuffix}" }
        }
        from("THIRD_PARTY_NOTICES.md")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = (project.property("java_version") as String).toInt()
    }
}
