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
sealed abstract trait KavachTransformer

/** NamedKavachTransformer is a sub-class of KavachTransformer that
 *  identifies functions by their name.
 */
abstract class NamedKavachTransformer(val name: String)
  extends KavachTransformer
{
  override def equals(that: Any) : Boolean = {
    that match {
      case that: NamedKavachTransformer => name == that.name
      case _ => false
    }
  }
}

/** NamedKavachTransformer1 is a sub-class of NamedKavachTransformer
 *  that applies a function on one KavachData.
 */
case class NamedKavachTransformer1[U, V](n : String, f : (U => V))
  extends NamedKavachTransformer(n)
{
  def apply(data : U) : V = f(data)
}

/** Base class for KavachDeclassifier, which will be applied on data that has
 *  policies associated with it.
 */ 
abstract trait KavachDeclassifier

/** NamedKavachDeclassifer is a sub-class of KavachDeclassifer that
 *  identifies functions by their name.
 */
abstract class NamedKavachDeclassifier(val name: String)
  extends KavachDeclassifier
{
  override def equals(that: Any) : Boolean = {
    that match {
      case that: NamedKavachDeclassifier => name == that.name
      case _ => false
    }
  }
}

/** NamedKavachDeclassifier1 is a sub-class of NamedKavachDeclassifier
 *  that applies a function on one KavachData.
 */
case class NamedKavachDeclassifier1[U, V](n : String, f : (U => V))
  extends NamedKavachDeclassifier(n) 
{
  def apply(data : U) : V = f(data)
}


/** This is the class that represents policies.
 */
case class PolicyFSM(
  currentSt : String,
  transitions : Map[(String, KavachTransformer), String],
  declassifications : Set[(String, KavachDeclassifier)]
)
{
  def transition(fnId : KavachTransformer) : PolicyFSM = {
    transitions.get((currentSt, fnId)) match {
      case Some(nextSt) => PolicyFSM(nextSt, transitions, declassifications)
      case None =>
        val msg = "Invalid policy transition in %s".format(currentSt)
        throw PolicyException(msg)
    }
  }
  def canDeclassify(fnId : KavachDeclassifier) : Boolean = {
    declassifications.contains((currentSt, fnId)) 
  }
}

/** This is the base class for all data which has a policy associated with it.
 */
class KavachData[T](val kavachStates : List[PolicyFSM], val data : T) {
}

/** Top-level Kavach data.
 */
object Kavach {
  def apply[U, V](wrapper : KavachData[U], tx: NamedKavachTransformer1[U, V]) : KavachData[V] = {
    val endStates = wrapper.kavachStates.map(_.transition(tx))
    val newData = tx(wrapper.data)
    new KavachData[V](endStates, newData)
  }
  def declassify[U, V](wrapper : KavachData[U], f : NamedKavachDeclassifier1[U, V]) : V = {
    if (wrapper.kavachStates.forall(_.canDeclassify(f))) {
      f(wrapper.data)
    } else {
      throw PolicyException("Invalid declassification according to policy")
    }
  }
}
