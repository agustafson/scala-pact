package com.itv.scalapactcore.common

import scala.language.implicitConversions
import argonaut._
import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.InteractionMatchers.MatchingRules

import scalaz._
import Scalaz._

object ScalaPactJsonEquality {

  implicit def toJsonEqualityWrapper(json: Json): JsonEqualityWrapper = JsonEqualityWrapper(json)

  case class JsonEqualityWrapper(json: Json) {
    def =~(to: Json): MatchingRules => Boolean = matchingRules =>
      PermissiveJsonEqualityHelper.areEqual(
        matchingRules.map(p => p.filter(r => r._1.startsWith("$.body"))),
        json,
        to,
        ""
      )

    def =<>=(to: Json): Boolean => MatchingRules => Boolean = beSelectivelyPermissive => matchingRules =>
      StrictJsonEqualityHelper.areEqual(
        beSelectivelyPermissive,
        matchingRules.map(p => p.filter(r => r._1.startsWith("$.body"))),
        json,
        to,
        ""
      )
  }

}

object StrictJsonEqualityHelper {

  def areEqual(beSelectivelyPermissive: Boolean, matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareFields(beSelectivelyPermissive, matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(beSelectivelyPermissive, matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }
  }

  private def compareArrays(beSelectivelyPermissive: Boolean, matchingRules: MatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean = {
    (expectedArray |@| receivedArray) { (ja, ra) =>
      if (ja.length == ra.length) {
        ja.zip(ra).forall(pair => areEqual(beSelectivelyPermissive, matchingRules, pair._1, pair._2, accumulatedJsonPath))
      } else {
        false
      }
    } match {
      case Some(matches) => matches
      case None => false
    }
  }

  private def compareFields(beSelectivelyPermissive: Boolean, matchingRules: MatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean = {
    if (!expectedFields.forall(f => received.hasField(f))) false
    else {
      if(beSelectivelyPermissive) {
        expectedFields.forall { field =>

          (expected.field(field) |@| received.field(field)){ areEqual(beSelectivelyPermissive, matchingRules, _, _, accumulatedJsonPath) } match {
            case Some(bool) => bool
            case None => false
          }
        }
      } else {
        if (expected.objectFieldsOrEmpty.length == received.objectFieldsOrEmpty.length) {
          expectedFields.forall { field =>
            (expected.field(field) |@| received.field(field)) { (e, r) =>
              areEqual(beSelectivelyPermissive, matchingRules, e, r, accumulatedJsonPath)
            } match {
              case Some(bool) => bool
              case None => false
            }
          }
        } else {
          false
        }
      }
    }
  }

  private def compareValues(matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {

    val findMatchingRule = (m: Map[String, MatchingRule]) => m.find(r => r._1.endsWith(accumulatedJsonPath)).map(_._2)

    matchingRules >>= findMatchingRule match {
      case Some(rule) if rule.`match` == "type" =>
        expected.name == received.name

      case None =>
        expected == received
    }

  }

}

object PermissiveJsonEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareFields(matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }
  }

  private def compareArrays(matchingRules: MatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean =
    (expectedArray |@| receivedArray) { (ja, ra) =>
      ja.forall { jo =>
        ra.exists(ro => areEqual(matchingRules, jo, ro, accumulatedJsonPath))
      }
    } match {
      case Some(matches) => matches
      case None => false
    }


  private def compareFields(matchingRules: MatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean =
    if(!expectedFields.forall(f => received.hasField(f))) false
    else {
      expectedFields.forall { field =>

        (expected.field(field) |@| received.field(field)){ (e, r) => areEqual(matchingRules, e, r, accumulatedJsonPath + s".${field.toString}") } match {
          case Some(bool) => bool
          case None => false
        }
      }
    }

  private def compareValues(matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {

    val findMatchingRule = (m: Map[String, MatchingRule]) => m.find(r => r._1.endsWith(accumulatedJsonPath)).map(_._2)

    matchingRules >>= findMatchingRule match {
      case Some(rule) if rule.`match` == "type" =>
        expected.name == received.name

      case None =>
        expected == received
    }

  }

}
