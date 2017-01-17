enablePlugins(JavaAppPackaging)

packageName in Universal := name.value + '-' + version.value
executableScriptName := name.value
makeBatScript := None // Disable generating of .bat script

/*
  More settings can come here. Please refer to http://www.scala-sbt.org/sbt-native-packager/formats/universal.html
 */
