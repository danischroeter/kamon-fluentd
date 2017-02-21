/* =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */


val akkaVersion210    = "2.3.16"
val akkaVersion212    = "2.4.16"

val kamonCore         = "io.kamon"                  %%  "kamon-core"            % "0.6.6"
val fluentdLogger     = "org.fluentd"               %%  "fluent-logger-scala"   % "0.7.0"
val easyMock          = "org.easymock"              %   "easymock"              % "3.2"

name := "kamon-fluentd"

parallelExecution in Test in Global := false

crossScalaVersions := Seq("2.11.8", "2.12.1")

libraryDependencies ++=
  compileScope(kamonCore, akkaDependency("actor").value, fluentdLogger) ++
  testScope(scalatest, akkaDependency("testkit").value, easyMock, slf4jApi, slf4jnop)

resolvers += Resolver.bintrayRepo("kamon-io", "releases")
