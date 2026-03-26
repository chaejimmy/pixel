import com.shourov.apps.pacedream.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("dagger.hilt.android.plugin")
            }

            dependencies {
                "implementation"(libs.findLibrary("hilt.android").get())
                "ksp"(libs.findLibrary("hilt.compiler").get())
                // Override kotlin-metadata-jvm for Kotlin 2.3 support (Hilt 2.58 bundles 2.2.x)
                "ksp"(libs.findLibrary("kotlin.metadata.jvm").get())
            }

        }
    }
}
