import shapeless._

object CaseClassCreator {
    import scala.reflect.macros.whitebox.Context
    import scala.language.experimental.macros

    def mkCaseClass[R <: HList]: Any =
      macro MkCaseClass.mkCaseClassImpl[R]

    class MkCaseClass(val c : Context) extends SingletonTypeUtils {
      def mkCaseClassImpl[R <: HList](implicit tag: c.universe.WeakTypeTag[R]) = {
        import c.universe._

        val rTpe = weakTypeOf[R]
        val keyTagTpe = typeOf[shapeless.labelled.KeyTag[_, _]]
        val hconsTpe = typeOf[shapeless.::[_, _]]

        def tagAndType(tpe: Type): (String, Type) =
          tpe.baseType(keyTagTpe.typeSymbol).typeArgs match {
            case List(SingletonSymbolType(s), fieldTpe) => (s, fieldTpe)
            case _ => c.abort(c.enclosingPosition, s"Malformed record entry type $tpe")
          }

        def mkMember(entryTpe: Type): Tree = {
          val (tag, tpe) = tagAndType(entryTpe)
          q"val ${TermName(tag)} : ${tpe}"
        }

        def foldEntries[A](tpe: Type)(s: A)(f: (A, Type) => A): A = {
          val tpeDealiased = tpe.dealias
          if (tpeDealiased =:= typeOf[HNil]) {
            s
          } else {
            tpe.baseType(hconsTpe.typeSymbol).typeArgs match {
              case List(entry, tl) => foldEntries(tl)(f(s, entry))(f)
              case _ => c.abort(c.enclosingPosition, s"Malformed hlist type $tpeDealiased")
            }
          }
        }

        def mkClass(rTpe: Type): Tree = {
          val members = foldEntries(rTpe)(List[Tree]())((s, tpe) => s :+ mkMember(tpe))
          val name = TypeName("FromRecord")
          q"case class $name(..$members)"
        }

        val freshName = c.freshName("CaseClassCreator")
        val freshTy = TypeName(freshName)
        val freshTerm = TermName(freshName)
        val result = c.Expr[Any](
          q"""
           class ${freshTy} {
             ${mkClass(rTpe)}
           }
           new ${freshTy}
          """)
        println(showCode(result.tree))
        result
      }
    }
}

