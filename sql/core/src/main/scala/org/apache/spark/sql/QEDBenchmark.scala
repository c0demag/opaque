/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql

import scala.util.Random

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import org.apache.spark.sql.QEDOpcode._
import org.apache.spark.sql.functions.substring
import org.apache.spark.sql.types.BinaryType
import org.apache.spark.sql.types.DateType
import org.apache.spark.sql.types.FloatType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StructType

object QEDBenchmark {
  import QED.time

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("QEDBenchmark")
    val sc = new SparkContext(sparkConf)
    val sqlContext = new SQLContext(sc)

    // time("spark sql sort") {
    //   QEDBenchmark.sortSparkSQL(sqlContext, 256 * 1024)
    // }

    // time("opaque sort") {
    //   QEDBenchmark.sortOpaque(sqlContext, 256 * 1024)
    // }

    QEDBenchmark.bd1SparkSQL(sqlContext, "1million")

    QEDBenchmark.bd1Opaque(sqlContext, "1million")

    // test("big data 2 - spark sql") {
    //   QEDBenchmark.bd2SparkSQL(sqlContext, "1node")
    // }

    // test("big data 2") {
    //   QEDBenchmark.bd2Opaque(sqlContext, "1node")
    // }

    sc.stop()
  }

  def bd1SparkSQL(sqlContext: SQLContext, size: String) {
    import sqlContext.implicits._
    val rankingsDF = sqlContext.read.schema(
      StructType(Seq(
        StructField("pageURL", StringType),
        StructField("pageRank", IntegerType),
        StructField("avgDuration", IntegerType))))
      .csv(s"/home/ankurd/big-data-benchmark-files/rankings/$size")
      .coalesce(2)
    val result = time("big data 1 - spark sql") {
      val df = rankingsDF.filter($"pageRank" > 1000).select($"pageURL", $"pageRank")
      val count = df.count
      println("big data 1 spark sql - num rows: " + count)
      df
    }
  }

  def bd1Opaque(sqlContext: SQLContext, size: String) {
    import sqlContext.implicits._
    val rankingsDF = sqlContext.read.schema(
      StructType(Seq(
        StructField("pageURL", StringType),
        StructField("pageRank", IntegerType),
        StructField("avgDuration", IntegerType))))
      .csv(s"/home/ankurd/big-data-benchmark-files/rankings/$size")
      .mapPartitions(QED.bd1Encrypt3)
      .toDF("pageURL", "pageRank", "avgDuration")
      .coalesce(2)
    val result = time("big data 1") {
      val df = rankingsDF.encFilter(OP_BD1).select($"pageURL", $"pageRank")
      val count = df.count
      println("big data 1 - num rows: " + count)
      df
    }
  }

  def bd2SparkSQL(sqlContext: SQLContext, size: String) {
    import sqlContext.implicits._
    val uservisitsDF = sqlContext.read.schema(
      StructType(Seq(
        StructField("sourceIP", StringType),
        StructField("destURL", StringType),
        StructField("visitDate", DateType),
        StructField("adRevenue", FloatType),
        StructField("userAgent", StringType),
        StructField("countryCode", StringType),
        StructField("languageCode", StringType),
        StructField("searchWord", StringType),
        StructField("duration", IntegerType))))
      .csv(s"/home/ankurd/big-data-benchmark-files/uservisits/$size")
      .coalesce(2)
    val result = time("big data 2 - spark sql") {
      val df = uservisitsDF.select(substring($"sourceIP", 0, 3).as("sourceIPSubstr"), $"adRevenue")
        .groupBy($"sourceIPSubstr").sum("adRevenue")
      val count = df.count
      println("big data 2 spark sql - num rows: " + count)
      df
    }
  }

  def bd2Opaque(sqlContext: SQLContext, size: String) {
    import sqlContext.implicits._
    val uservisitsDF = sqlContext.read.schema(
      StructType(Seq(
        StructField("sourceIP", StringType),
        StructField("destURL", StringType),
        StructField("visitDate", DateType),
        StructField("adRevenue", FloatType),
        StructField("userAgent", StringType),
        StructField("countryCode", StringType),
        StructField("languageCode", StringType),
        StructField("searchWord", StringType),
        StructField("duration", IntegerType))))
      .csv(s"/home/ankurd/big-data-benchmark-files/uservisits/$size")
      .mapPartitions(QED.bd2Encrypt9)
      .toDF("sourceIP", "destURL", "visitDate",
        "adRevenue", "userAgent", "countryCode",
        "languageCode", "searchWord", "duration")
      .coalesce(2)
    val result = time("big data 2") {
      val df = uservisitsDF.select($"sourceIP", $"adRevenue").encProject($"sourceIP", $"adRevenue")
        .encGroupByWithSum($"sourceIP", $"adRevenue".as("totalAdRevenue"))
      val count = df.count
      println("big data 2 - num rows: " + count)
      df
    }
  }

  def sortSparkSQL(sqlContext: SQLContext, n: Int) {
    import sqlContext.implicits._
    val data = Random.shuffle((0 until n).map(x => (x.toString, x)).toSeq)
    val sorted = time("spark sql sorting") {
      val df = sqlContext.sparkContext.makeRDD(data).toDF("str", "x").sort($"x")
      df.count()
      df
    }
  }

  def sortOpaque(sqlContext: SQLContext, n: Int) {
    import sqlContext.implicits._
    val data = Random.shuffle((0 until n).map(x => (x.toString, x)).toSeq)
    val sorted = time("Enc sorting: ") {
      val df = sqlContext.sparkContext.makeRDD(QED.encrypt2(data)).toDF("str", "x").encSort($"x")
      df.count()
      df
    }
  }
}