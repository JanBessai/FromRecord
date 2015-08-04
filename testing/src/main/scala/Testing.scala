object Testing {
  import shapeless._
  import syntax.singleton._
  import record._
  type R = Record.`'x -> Int, 'y -> String`.T
  val rt = CaseClassCreator.mkCaseClass[R]
  type FromR = rt.FromRecord
}

