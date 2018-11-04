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

package edu.berkeley.cs.rise.opaque.kavach

/**
 * This is the exception raised when the policy is not followed.
 */
final case class PolicyException(
    private val message: String,
    private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
 * This is the exception raised when a function is not implemented.
 */
final case class NotImplementedException(
    private val message: String,
    private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

/** Base class for KavachTransformer, which will be applied on data that has
 *  policies associated with it.
 * 
 * One can imagine various types of function applications identified by
 * measurements, credentials, etc. Right now the only sub-class supported
 * NamedKavachTransformer, which identifies functions using strings that
 * represent their names.
 */
sealed abstract trait KavachTransformer {
  def apply(data : KavachData) : KavachData = {
    throw new NotImplementedException("apply not implemented")
  }

  def apply2(d1 : KavachData, d2 : KavachData) : KavachData = {
    throw new NotImplementedException("apply2 not implemented")
  }
  // TODO: more such functions ...
}

/** NamedKavachTransformer is a sub-class of KavachTransformer that
 *  identifies functions by their name.
 */
trait NamedKavachTransformer extends KavachTransformer {
  val name: String
  override def equals(that: Any) : Boolean = {
    that match {
      case that: NamedKavachTransformer => name == that.name
      case _ => false
    }
  }
}

/** Base class for KavachDeclassifier, which will be applied on data that has
 *  policies associated with it.
 */ 
sealed abstract trait KavachDeclassifier {
  def declassify(data : KavachData) : Any = {
    throw new NotImplementedException("declassify not implemented")
  }
  // TODO: more such functions ...
}

/** NamedKavachDeclassifer is a sub-class of KavachDeclassifer that
 *  identifies functions by their name.
 */
trait NamedKavachDeclassifier extends KavachDeclassifier {
  val name: String
  override def equals(that: Any) : Boolean = {
    that match {
      case that: NamedKavachDeclassifier => name == that.name
      case _ => false
    }
  }
}

/** Each state in the policy state machine.
 */
trait PolicyState {
  val name: String
  val transitions: List[(KavachTransformer, PolicyState)]
  val declassifications: List[KavachDeclassifier]

  override def toString = "PolicyState(%s)".format(name)
}

/** This is the class that represents policies.
 */
case class PolicyFSM(currentState : PolicyState) {
  def transition(fnId : KavachTransformer) : PolicyFSM = {
    val nextStates = currentState.transitions.find(p => p._1.equals(fnId))
    nextStates match {
      case Some(p) => PolicyFSM(p._2)
      case None =>
        val msg = "Invalid policy transition in %s".format(currentState.toString)
        throw PolicyException(msg)
    }
  }
}

/** This is the base class for all data which has a policy associated with it.
 */
trait KavachData {
  val kavachState : PolicyFSM
  def withPolicyState(st : PolicyFSM) : KavachData
}

/** Top-level Kavach data.
 */
object Kavach {
  def apply(data : KavachData, f : KavachTransformer) : KavachData = {
    val startState = data.kavachState
    val endState = startState.transition(f)
    val newData = f.apply(data)
    val newDataP = newData.withPolicyState(endState)
    if(newDataP.kavachState != endState) {
      throw new PolicyException("Invalid policy end-state.")
    }
    newDataP
  }
  def declassify(data : KavachData, f : KavachDeclassifier) : Any = {
    val st = data.kavachState.currentState
    if (st.declassifications.find(p => p.equals(f)).isDefined) {
      f.declassify(data)
    } else {
      val msg = "Invalid policy declassification: %s".format(st.toString)
      throw new PolicyException(msg)
    }
  }
}
