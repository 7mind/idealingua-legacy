package izumi.idealingua.model.publishing.manifests

import izumi.idealingua.model.publishing.BuildManifest.Common
import izumi.idealingua.model.publishing.{BuildManifest, ProjectNamingRule}

case class ScalaBuildManifest(
  common: Common,
  layout: ScalaProjectLayout,
  sbt: SbtOptions,
) extends BuildManifest

case class SbtOptions(
  projectNaming: ProjectNamingRule,
  enableScalaJs: Boolean,
  scalaVersion: Option[String],
  sbtVersion: Option[String],
  enableDocs: Option[Boolean],
)

object SbtOptions {
  def example: SbtOptions = {
    SbtOptions(
      projectNaming = ProjectNamingRule.example,
      enableScalaJs = true,
      scalaVersion  = None,
      sbtVersion    = None,
      enableDocs    = None,
    )
  }
}

object ScalaBuildManifest {
  def example: ScalaBuildManifest = {
    val common = BuildManifest.Common.example
    ScalaBuildManifest(
      common = common.copy(version = common.version.copy(snapshotQualifier = "SNAPSHOT")),
      layout = ScalaProjectLayout.SBT,
      sbt    = SbtOptions.example,
    )
  }
}

sealed trait ScalaProjectLayout

object ScalaProjectLayout {

  final case object PLAIN extends ScalaProjectLayout

  final case object SBT extends ScalaProjectLayout

}
