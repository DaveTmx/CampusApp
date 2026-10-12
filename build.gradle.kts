// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

// Build output lives outside the project folder, in <your user folder>\.campusapp-build.
// OneDrive (and other sync tools) lock files in app\build while uploading them, which makes
// Clean / Rebuild fail with "Unable to delete directory ... a process has files open".
// The folder name includes a code made from the project path, so two copies of the project
// never share build output.
val outsideBuildRoot = file(System.getProperty("user.home") + "/.campusapp-build/" +
        rootProject.name + "-" + Integer.toHexString(rootDir.absolutePath.hashCode()))
allprojects {
    layout.buildDirectory.set(File(outsideBuildRoot, if (this == rootProject) "root" else name))
}
