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

/** Base class for DataTransformer, which will be applied on data that has
 *  policies associated with it.
 * 
 * One can imagine various types of function applications identified by
 * measurements, credentials, etc. Right now the only sub-class supported
 * NamedDataTransformer, which identifies functions using strings that
 * represent their names.
 */
sealed abstract class DataTransformer

/** NamedDataTransformer is a sub-class of FunctionIdentifier that
 *  identifies functions by their name.
 *
 */
case class NamedDataTransformer(name: String) extends DataTransformer {
  override def equals(that: Any) : Boolean = {
    that match {
      case that: NamedDataTransformer => name == that.name
      case _ => false
    }
  }
}

/**
 * This is the exception raised when the policy is not followed.
 */
final case class PolicyException(
    private val message: String,
    private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
 * Each state in the policy state machine.
 */
case class PolicyState(
  name: String, transitions: List[(DataTransformer, PolicyState)])
{
  override def toString = "PolicyState(%s)".format(name)
}

/**
 * This is the class that represents policies.
 */
case class PolicyFSM(currentState : PolicyState) {
  def transition(fnId : DataTransformer) : PolicyFSM = {
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
trait PolicyEnforcingData {
  var currentPolicyState : PolicyFSM
}
