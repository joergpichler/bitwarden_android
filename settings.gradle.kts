import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

/**
 * Loads local user-specific build properties that are not checked into source control.
 */
val userProperties = Properties().apply {
    val buildPropertiesFile = File(rootDir, "user.properties")
    if (buildPropertiesFile.exists()) {
        FileInputStream(buildPropertiesFile).use { load(it) }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages (Bitwarden)"
            url = uri("https://maven.pkg.github.com/bitwarden/sdk")
            credentials {
                // GitHub Packages returns 401 when the username is blank, even with a valid token.
                username = (userProperties["gitHubUser"] as String?)
                    ?.takeIf { it.isNotBlank() }
                    ?: System.getenv("GITHUB_ACTOR")?.takeIf { it.isNotBlank() }
                    ?: "token"
                password = (userProperties["gitHubToken"] as String?)
                    ?.takeIf { it.isNotBlank() }
                    ?: System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() }
                    ?: System.getenv("GH_TOKEN")?.takeIf { it.isNotBlank() }
                    ?: error(
                        "Missing GitHub Packages token for com.bitwarden:sdk-android. " +
                            "Create a classic PAT with the read:packages scope at " +
                            "https://github.com/settings/tokens then set gitHubToken in " +
                            "user.properties or the GITHUB_TOKEN environment variable.",
                    )
            }
        }
        if ((userProperties["localSdk"] as String?).toBoolean()) {
            mavenLocal()
        }
    }
}

rootProject.name = "Bitwarden"
include(
    ":annotation",
    ":app",
    ":authenticator",
    ":authenticatorbridge",
    ":core",
    ":cxf",
    ":data",
    ":network",
    ":testharness",
    ":ui",
)
