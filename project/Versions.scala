object V {
  // foundation

  val scalatest = "3.2.18"

  val http4s       = "0.23.27"
  val http4s_blaze = "0.23.16"

  val scalameta = "4.9.5" // Not available for Scala 3 yet
  val fastparse = "3.1.0" // 3.0.0 is available for Scala 3

  val scala_xml = "2.3.0"

  val kind_projector = "0.13.3"

  val circe_derivation     = "0.13.0-M5"
  val circe_generic_extras = "0.14.4"

  val scala_java_time = "2.6.0"

  // java-only dependencies below
  // java, we need it bcs http4s ws client isn't ready yet
  val asynchttpclient = "2.12.3"

  val slf4j           = "1.7.30"
  val typesafe_config = "1.4.3"
}
