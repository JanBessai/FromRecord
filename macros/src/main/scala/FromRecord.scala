import shapeless._

object CaseClassCreator {
    import scala.reflect.macros.whitebox.Context
    import scala.language.experimental.macros
    import scala.annotation.StaticAnnotation
    import scala.annotation.compileTimeOnly

    @compileTimeOnly("Run via macro paradise")
    class FromRecord[R <: HList] extends StaticAnnotation {
      def macroTransform(annottees: Any*): Any =
        macro MkCaseClass.mkCaseClassImpl
    }

    class MkCaseClass(val c : Context) extends SingletonTypeUtils {
      def mkCaseClassImpl(annottees: c.Expr[Any]*) = {
        import c.universe._

        val q"new FromRecord[${rTpeTree : Tree}]()" = c.prefix.tree //"
        val rTpeTreeChecked = c.typecheck(rTpeTree, mode = c.TYPEmode)
        val rTpe : Type= rTpeTreeChecked.symbol.typeSignature
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

        annottees.map(_.tree) match {
          case List(q"object $name extends $parent { ..$body}") if body.isEmpty =>
            q"object $name extends $parent { ${mkClass(rTpe)} }"
          case _ => c.abort(c.enclosingPosition,
            "From record can only be applied to objects with empty bodies")
        }
      }
    }
}

