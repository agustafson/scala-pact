package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

class IrNodePathSpec extends FunSpec with Matchers {

  describe("Build IrNodePath's") {

    it("should be able to represent an empty path") {
      val path: IrNodePath = IrNodePathEmpty

      path.renderAsString shouldEqual "."
    }

    it("should be able to represent going down through object fields") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips"

      path.renderAsString shouldEqual ".fish.chips"
    }

    it("should be able to represent going down into an array") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup"

      path.renderAsString shouldEqual ".fish.chips[2].ketchup"
    }

    it("should be able to represent any element in an array") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup"

      path.renderAsString shouldEqual ".fish.chips[*].ketchup"
    }

    it("should be able to represent a path to an xml attribute") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup" <@ "applied"

      path.renderAsString shouldEqual ".fish.chips[*].ketchup['@applied']"
    }

    it("should be able to represent a path to an xml text element") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup" text

      path.renderAsString shouldEqual ".fish.chips[*].ketchup['#text']"
    }

  }

  describe("converting IrNodePath to and from PactPath") {

    it("should be able to convert to and from dot syntax") {

      val jsonPath = ".animals[*].dogs[2].collie.rover['@name']"

      val expected = IrNodePathEmpty <~ "animals" <~ "*" <~ "dogs" <~ 2 <~ "collie" <~ "rover" <@ "name"

      val nodePath = PactPath.fromPactPath(jsonPath).getOrElse(fail("Could not parse path"))

      withClue("Created paths are equal") {
        println(nodePath)
        println(expected)
        nodePath === expected shouldEqual true
      }

      withClue("Rendered path are as expected") {
        nodePath.renderAsString shouldEqual jsonPath
      }

    }

    it("should be able to convert to and from bracket syntax") {

      val jsonPathA = "['animals'][*]['dogs'][2]['collie']['rover']['@name']"
      val jsonPathB = """["animals"][*]["dogs"][2]["collie"]["rover"]["@name"]"""

      val expected = IrNodePathEmpty <~ "animals" <~ "*" <~ "dogs" <~ 2 <~ "collie" <~ "rover" <@ "name"

      withClue("Single and double quotes are equivalent") {
        PactPath.fromPactPath(jsonPathA) === PactPath.fromPactPath(jsonPathB) shouldEqual true
      }

      val nodePath = PactPath.fromPactPath(jsonPathA).getOrElse(fail("Could not parse path"))

      withClue("Created paths are equal") {
        nodePath === expected shouldEqual true
      }

      val expectedRender = ".animals[*].dogs[2].collie.rover['@name']" // renders as dot syntax

      withClue("Rendered path are as expected") {
        nodePath.renderAsString shouldEqual expectedRender
      }

    }

    it("should be able to convert to and from a combination of dot and bracket syntax") {

      val jsonPath = ".animals[*].dogs[2]['collie'].rover['@name']"

      val expected = IrNodePathEmpty <~ "animals" <~ "*" <~ "dogs" <~ 2 <~ "collie" <~ "rover" <@ "name"

      val nodePath = PactPath.fromPactPath(jsonPath).getOrElse(fail("Could not parse path"))

      withClue("Created paths are equal") {
        nodePath === expected shouldEqual true
      }

      val expectedRender = ".animals[*].dogs[2].collie.rover['@name']" // renders as dot syntax

      withClue("Rendered path are as expected") {
        nodePath.renderAsString shouldEqual expectedRender
      }

    }

  }

  describe("comparing two IrNodePaths") {

    it("should be able to test that equal paths are equal") {

      IrNodePathEmpty === IrNodePathEmpty shouldEqual true
      IrNodePathEmpty === (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") shouldEqual false
      (IrNodePathEmpty <~ "fish" <~ "chips") === (IrNodePathEmpty <~ "fish" <~ "chips") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") shouldEqual true

    }

    it("should be able to test that references to 'any element' are accepted") {

      (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ 1 <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") shouldEqual true

    }

    it("should be able to check paths with attributes") {
      val expected =
        IrNodePathFieldAttribute(
          "salt",
          IrNodePathField(
            "chips",
            IrNodePathField(
              "fish",
              IrNodePathEmpty
            )
          )
        )

      (IrNodePathEmpty <~ "fish" <~ "chips" <@ "salt") === expected shouldEqual true
    }

    it("should be able to check paths with text elements") {
      val expected =
        IrNodePathTextElement(
          IrNodePathField(
            "chips",
            IrNodePathField(
              "fish",
              IrNodePathEmpty
            )
          )
        )

      (IrNodePathEmpty <~ "fish" <~ "chips" text) === expected shouldEqual true
    }

  }

}