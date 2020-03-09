import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group = "org.dicthub"
version = "1.0-SNAPSHOT"

val kotlinVersion = "1.3.70"
val kotlinHtmlVersion = "0.7.1"

val SourceSet.kotlin: SourceDirectorySet
    get() =
        (this as HasConvention)
                .convention
                .getPlugin(KotlinSourceSet::class.java)
                .kotlin

plugins {
    id("kotlin2js") version "1.3.70"
     `maven-publish`
}

repositories {
    jcenter()
}

dependencies {
    compileOnly(kotlin("stdlib-js"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinHtmlVersion")

    testCompileOnly("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
}

sourceSets {
    main {
        kotlin.srcDir(rootProject.file("plugin-shared-utility/src/main/kotlin"))
    }
}

tasks {
    compileKotlin2Js {
        kotlinOptions {
            sourceMap = true
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("js") {
            artifact(file("${project.buildDir}/classes/kotlin/main/${project.name}.js"))
        }
    }
    repositories {
        maven {
            url = uri("s3://org.dicthub.plugins.autopublish/maven2")
            credentials(AwsCredentials::class) {
                accessKey = System.getenv("AWS_ACCESS_KEY_ID")
                secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
            }
        }
    }
}
tasks["publish"].dependsOn("build")