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
  case class VoteData(id : Integer, choice : String) extends KavachData {
    override val kavachState = PolicyFSM(VotePolicyS1)
    override def withPolicyState(st : PolicyFSM) = this
  }

  case object GetVoteId extends NamedKavachDeclassifier {
    override val name = "GetVoteId"
    override def declassify(vote : KavachData) : Integer =
      vote.asInstanceOf[VoteData].id
  }

  case object GetVoteChoice extends NamedKavachDeclassifier {
    override val name = "GetVoteChoice"
    override def declassify(vote : KavachData) : String =
      vote.asInstanceOf[VoteData].choice
  }

  case object VotePolicyS1 extends PolicyState
  {
    override val name = "S1"
    override val transitions = List.empty
    override val declassifications = List(GetVoteId, GetVoteChoice)
  }


  def main(args: Array[String]): Unit = {
    val vote = VoteData(1, "opaque")
    println(Kavach.declassify(vote, GetVoteChoice))
  }
}
