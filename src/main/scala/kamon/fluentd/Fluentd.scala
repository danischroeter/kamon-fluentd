/* =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
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

package kamon.fluentd

import java.time.Instant

import com.typesafe.config.Config
import kamon.fluentd.FluentdReporter._
import kamon.metric.PeriodSnapshot
import kamon.{Kamon, MetricReporter}
import org.fluentd.logger.scala.FluentLogger
import org.fluentd.logger.scala.sender.ScalaRawSocketSender
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


class FluentdReporter private[fluentd](private var f: FluentdWriter) extends MetricReporter {

  def this() = this(new FluentdWriter(FluentdReporter.readConfiguration(Kamon.config())))

  //  def buildMetricsListener(flushInterval: FiniteDuration, tickInterval: FiniteDuration,
  //                           tag: String, host: String, port: Int,
  //                           histogramStatsConfig: HistogramStatsConfig): ActorRef = {
  //    assert(flushInterval >= tickInterval, "Fluentd flush-interval needs to be equal or greater to the tick-interval")
  //
  //    val metricsSender = system.actorOf(
  //      Props(new FluentdMetricsSender(tag, host, port, histogramStatsConfig)),
  //      "kamon-fluentd")
  //    if (flushInterval == tickInterval) {
  //      metricsSender
  //    } else {
  //      system.actorOf(TickMetricSnapshotBuffer.props(flushInterval, metricsSender), "kamon-fluentd-buffer")
  //    }
  //  }
  override def start(): Unit = logger.info("Started the Kamon Fluentd reporter")
  override def stop(): Unit = logger.info("Stopped the Kamon Fluentd reporter")
  override def reconfigure(config: Config): Unit = {
    f = new FluentdWriter(FluentdReporter.readConfiguration(config()))
  }

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    val time = snapshot.to
    for (counter <- snapshot.metrics.counters) {
      //config.packetBuffer.appendMeasurement(counter.name, config.measurementFormatter.formatMeasurement(encodeDatadogCounter(counter.value, counter.unit), counter.tags))

      //      val fluentdTagName = if (isSingleInstrumentEntity(entity)) {
      //        s"$appName.${entity.category}.${entity.name}"
      //      } else {
      //        s"$appName.${entity.category}.${entity.name}.${metricKey.name}"
      //      }
      //    }

      val attrs = Map(
        "app" -> f.config.appName,
        "metric" -> counter.name,
        "unit" -> counter.unit,
        "value" -> counter.value,
        "tags" -> counter.tags)

      //todo allow to config if 0 is sent
      //                if (cs.count > 0) {
      log_fluentd(time, fluentdTagName, "count", cs.count, attrs)
      //                  fluentd.flush()
      //                }


    }

    for (gauge <- snapshot.metrics.gauges) {
      //config.packetBuffer.appendMeasurement(gauge.name, config.measurementFormatter.formatMeasurement(encodeDatadogGauge(gauge.value, gauge.unit), gauge.tags))

    }

    for (
      metric <- snapshot.metrics.histograms ++ snapshot.metrics.rangeSamplers;
      bucket <- metric.distribution.bucketsIterator
    ) {

      //      val bucketData = config.measurementFormatter.formatMeasurement(encodeDatadogHistogramBucket(bucket.value, bucket.frequency, metric.unit), metric.tags)
      //      config.packetBuffer.appendMeasurement(metric.name, bucketData)
    }

    //strategy to flush each or send per batch ->batch default...
    f.fluentd.flush()

    //    val time = snapshot.to
    //    for {
    //      (groupIdentity, groupSnapshot) ← snapshot.metrics
    //      (metricIdentity, metricSnapshot) ← groupSnapshot.metrics
    //    } {
    //
    //      val fluentdTagName = fluentdTagNameFor(groupIdentity, metricIdentity)
    //
    //      val attrs = Map(
    //        "app.name" -> appName,
    //        "category.name" -> groupIdentity.category,
    //        "entity.name" -> groupIdentity.name,
    //        "metric.name" -> metricIdentity.name,
    //        "unit_of_measurement.name" -> metricIdentity.unitOfMeasurement.name,
    //        "unit_of_measurement.label" -> metricIdentity.unitOfMeasurement.label) ++ groupIdentity.tags.map(kv ⇒ s"tags.${kv._1}" -> kv._2)
    //
    //      metricSnapshot match {
    //        case hs: Histogram.Snapshot ⇒
    //          if (hs.numberOfMeasurements > 0) {
    //            histogramStatsBuilder.buildStats(hs) foreach {
    //              case (_name, value) ⇒
    //                log_fluentd(time, fluentdTagName, _name, value, attrs)
    //            }
    //            fluentd.flush()
    //          }
    //        case cs: Counter.Snapshot ⇒
    //          if (cs.count > 0) {
    //            log_fluentd(time, fluentdTagName, "count", cs.count, attrs)
    //            fluentd.flush()
    //          }
    //      }
    //    }
  }

  private def log_fluentd(time: Instant, fluentdTagName: String, statsName: String, value: Any, attrs: Map[String, String] = Map.empty) = {
    f.fluentd.log(
      fluentdTagName,
      attrs ++ Map(
        "stats.name" -> statsName,
        "value" -> value,
        "canonical_metric.name" -> (fluentdTagName + "." + statsName),
        (fluentdTagName + "." + statsName) -> value),
      time.getEpochSecond)
  }
}

object FluentdReporter {
  private val logger = LoggerFactory.getLogger(classOf[FluentdReporter])

  private[fluentd] def readConfiguration(config: Config): Configuration = {
    val fluentdConfig = config.getConfig("kamon.fluentd")
    val host = fluentdConfig.getString("hostname")
    val port = fluentdConfig.getInt("port")

    val appName = config.getString("application-name")
    val tag = fluentdConfig.getString("tag")

//    val subscriptions = fluentdConfig.getConfig("subscriptions")
//    val histogramStatsConfig = new HistogramStatsConfig(
//      fluentdConfig.getStringList("histogram-stats.subscription").asScala.toList,
//      fluentdConfig.getDoubleList("histogram-stats.percentiles").asScala.toList.map(_.toDouble))
    //    val subscriber = buildMetricsListener(flushInterval, tickInterval, tag, host, port, histogramStatsConfig) todo
    //    subscriptions.firstLevelKeys foreach { subscriptionCategory ⇒
    //      subscriptions.getStringList(subscriptionCategory).asScala.foreach { pattern ⇒
    //        Kamon.metrics.subscribe(subscriptionCategory, pattern, subscriber, permanently = true)
    //      }
    //    }
    Configuration(host, port, tag, appName)
  }
}


private[fluentd] class FluentdWriter(val config: Configuration) {
  lazy val fluentd: FluentLogger = FluentLogger(config.tag, new ScalaRawSocketSender(config.host, config.port, 3 * 1000, 1 * 1024 * 1024))
}
private[fluentd] case class Configuration(host: String, port: Int,
                                          tag: String, appName: String
                                          //subscriptions:Config,histogramStatsConfig:HistogramStatsConfig
                                         )

//case class HistogramStatsBuilder(config: HistogramStatsConfig) {
//  import HistogramStatsBuilder.RichHistogramSnapshot
//  import HistogramStatsConfig._
//
//  // this returns List of ("statsName", "value as String")
//  def buildStats(hs: Histogram.Snapshot): List[(String, Any)] = {
//    config.subscriptions.foldRight(List.empty[(String, Any)]) { (name, res) ⇒
//      name match {
//        case COUNT ⇒ (name, hs.numberOfMeasurements) :: res
//        case MAX ⇒ (name, hs.max) :: res
//        case MIN ⇒ (name, hs.min) :: res
//        case AVERAGE ⇒ (name, hs.average) :: res
//        case PERCENTILES ⇒ {
//          config.percentiles.foldRight(List.empty[(String, Any)]) { (p, _res) ⇒
//            val pStr = if (p.toString.matches("[0-9]+\\.[0]+")) p.toInt.toString else p.toString.replace(".", "_")
//            (name + "." + pStr, hs.percentile(p)) :: _res
//          } ++ res
//        }
//      }
//    }
//  }
//}
//
//object HistogramStatsBuilder {
//
//  implicit class RichHistogramSnapshot(histogram: Histogram.Snapshot) {
//    def average: Double = {
//      if (histogram.numberOfMeasurements == 0) 0D
//      else histogram.sum / histogram.numberOfMeasurements
//    }
//  }
//
//}
//
//class HistogramStatsConfig(_subscriptions: List[String], _percentiles: List[Double]) {
//  import HistogramStatsConfig._
//  val subscriptions: List[String] = {
//    if (_subscriptions.contains("*")) {
//      supported
//    } else {
//      assert(_subscriptions.forall(supported.contains(_)), s"supported stats values are: ${supported.mkString(",")}")
//      _subscriptions
//    }
//  }
//  val percentiles: List[Double] = {
//    if (subscriptions.contains("percentiles")) {
//      assert(_percentiles.forall(p ⇒ 0.0 <= p && p <= 100.0), "every percentile point p must be 0.0 <= p <= 100.0")
//    }
//    _percentiles
//  }
//}
//
//object HistogramStatsConfig {
//  val COUNT = "count"
//  val MIN = "min"
//  val MAX = "max"
//  val AVERAGE = "average"
//  val PERCENTILES = "percentiles"
//  val supported = List(COUNT, MIN, MAX, AVERAGE, PERCENTILES)
//}
