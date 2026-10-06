plugins {
    java
    id("io.papermc.paperweight.userdev").version("2.0.0-beta.19")
    id("xyz.jpenilla.run-paper").version("3.0.2")
    id("com.gradleup.shadow").version("9.2.2")
}

group = "com.jnngl"
version = "1.0.1"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.21.10-R0.1-SNAPSHOT")
    implementation("net.elytrium:serializer:1.1.1")
    implementation("com.jnngl:mapcolor:1.0.1")
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    compileOnly("org.projectlombok:lombok:1.18.30")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("net.elytrium.serializer", "com.jnngl.vanillaminimaps.serializer")
        exclude("org/slf4j/**")
        minimize()
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}

val resourcePack = tasks.register<Zip>("resourcePack") {
    group = "build"
    description = "Packages the client-side minimap shaders as a resource pack."
    archiveFileName.set("VanillaMinimaps-resourcepack-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from("src/main/resourcepack")
    from("src/main/resources/shaders") {
        into("assets/minecraft/shaders")
    }
}

// shadowJar is already part of assemble since Shadow 9.
tasks.named("build") {
    dependsOn(resourcePack)
}