
import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.shourov.apps.pacedream.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val hasGoogleServices = file("google-services.json").exists()

            with(pluginManager) {
                // Only apply Firebase plugins when google-services.json exists; the
                // file is gitignored so fresh clones / CI builds without it still
                // succeed.  The firebase-perf plugin instruments OkHttp at bytecode
                // level, so applying it without a configured FirebaseApp causes a
                // fatal NoClassDefFoundError at runtime.
                if (hasGoogleServices) {
                    apply("com.google.gms.google-services")
                    apply("com.google.firebase.firebase-perf")
                    apply("com.google.firebase.crashlytics")
                }
            }

            dependencies {
                val bom = libs.findLibrary("firebase-bom").get()
                add("implementation", platform(bom))
                "implementation"(libs.findLibrary("firebase.analytics").get())
                "implementation"(libs.findLibrary("firebase.performance").get())
                "implementation"(libs.findLibrary("firebase.crashlytics").get())
            }

            if (hasGoogleServices) {
                extensions.configure<ApplicationExtension> {
                    buildTypes.configureEach {
                        // Disable the Crashlytics mapping file upload. This feature should only
                        // be enabled if a Firebase backend is available and configured in
                        // google-services.json.
                        configure<CrashlyticsExtension> {
                            mappingFileUploadEnabled = false
                        }
                    }
                }
            }
        }
    }
}
