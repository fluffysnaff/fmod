plugins {
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
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
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of((properties["java_version"] as String).toInt())
}

tasks {
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
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = (project.property("java_version") as String).toInt()
    }
}
