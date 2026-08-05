// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

// Project license metadata
// This makes the license visible to some tooling that reads Gradle extra properties.
// SPDX: GPL-3.0-only
extra["license"] = "GPL-3.0-only"
extra["licenseName"] = "GNU General Public License v3.0 (GPL-3.0-only)"
