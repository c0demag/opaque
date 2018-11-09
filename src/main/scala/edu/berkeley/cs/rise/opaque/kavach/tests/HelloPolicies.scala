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

import edu.berkeley.cs.rise.opaque.kavach._

object HelloPolicies
{
  case class VoteData(id : Integer, choice : String)

  def readVoteId(v : VoteData) : Integer = v.id
  def readVoteChoice(v : VoteData) : String = v.choice
  val deReadVoteId = NamedKavachDeclassifier1("readVoteId", readVoteId)
  val deReadVoteChoice = NamedKavachDeclassifier1("readVoteChoice", readVoteChoice)
  val fsm = List(PolicyFSM("S1", Map.empty, Set(("S2", deReadVoteId), ("S1", deReadVoteChoice))))


  def main(args: Array[String]): Unit = {
    val vote = new KavachData(fsm, VoteData(1, "opaque"))
    println("choice: " + Kavach.declassify(vote, deReadVoteChoice))
    // The next line will throw a policy exception because deReadVoteId is only
    // allowed in state S2, but we are currently in state S1.
    // println("id: " + Kavach.declassify(vote, deReadVoteId))
  }
}
