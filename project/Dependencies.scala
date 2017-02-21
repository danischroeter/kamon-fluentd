/* =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

import sbt._
import sbt.Keys._

object Dependencies {

  val resolutionRepos = Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Kamon Repository Snapshots" at "http://snapshots.kamon.io"
  )

  val kamonVersion      = "0.6.5"
  val akkaVersion210    = "2.3.16"
  val akkaVersion212    = "2.4.16"
  val slf4jVersion      = "1.7.7"

  val kamonCore         = "io.kamon"                  %%  "kamon-core"            % kamonVersion
  val kamonTestkit      = "io.kamon"                  %%  "kamon-testkit"         % kamonVersion

  val slf4jApi          = "org.slf4j"                 %   "slf4j-api"             % slf4jVersion
  val slf4jnop          = "org.slf4j"                 %   "slf4j-nop"             % slf4jVersion

  val fluentdLogger     = "org.fluentd"               %%  "fluent-logger-scala"   % "0.7.0"

  val scalatest         = "org.scalatest"             %%  "scalatest"             % "3.0.1"
  val easyMock          = "org.easymock"              %   "easymock"              % "3.2"


  def akkaDependency(moduleName: String) = Def.setting {
    scalaBinaryVersion.value match {
      case "2.10" | "2.11"  => "com.typesafe.akka" %% s"akka-$moduleName" % akkaVersion210
      case "2.12"           => "com.typesafe.akka" %% s"akka-$moduleName" % akkaVersion212
    }
  }

  def compileScope   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def testScope      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
}
