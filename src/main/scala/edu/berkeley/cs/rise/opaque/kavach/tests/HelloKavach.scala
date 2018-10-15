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

package edu.berkeley.cs.rise.opaque.kavach.tests

import edu.berkeley.cs.rise.opaque.implicits._
import edu.berkeley.cs.rise.opaque.Utils
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.lit

object HelloKavach {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KavachToyBenchmark1")
      .getOrCreate()
    import spark.implicits._

    // Initialize SQL context.
    Utils.initSQLContext(spark.sqlContext)

    // create a dataframe.
    val data = Seq(("foo", 4), ("bar", 1), ("baz", 5))
    val df = spark.createDataFrame(data).toDF("word", "count")
    // encrypt it.
    val dfEncrypted = df.encrypted
    // use it for processing.
    val result = dfEncrypted.filter($"count" > lit(3))
    // display
    result.show
    // now save it to a file.
    dfEncrypted.write.format("edu.berkeley.cs.rise.opaque.EncryptedSource").save("dfEncrypted")
    // read from a file.
    import org.apache.spark.sql.types._
    val df2 = (spark.read.format("edu.berkeley.cs.rise.opaque.EncryptedSource")
                .schema(StructType(Seq(StructField("word", StringType), StructField("count", IntegerType))))
                .load("dfEncrypted"))
    // display fields
    df2.show
  }
}
