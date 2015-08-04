object Testing {
  import shapeless._
  import syntax.singleton._
  import record._
  type R = Record.`'x -> Int, 'y -> String`.T

  // Wrapper object is necessary because of
  // https://github.com/aztek/scala-workflow/issues/2#issuecomment-31579391
  object MkClass {
    import CaseClassCreator._
    @FromRecord[R] object RC
  }

}
