package osp.leobert.android.reporter.diagram.core

import osp.leobert.android.maat.dag.DAG
import osp.leobert.android.maat.dag.Edge
import osp.leobert.android.reporter.diagram.Utils.ifElement
import osp.leobert.android.reporter.diagram.Utils.shouldIgnoreEmlElement
import osp.leobert.android.reporter.diagram.Utils.takeIfInstance
import osp.leobert.android.reporter.diagram.notation.ClassDiagram
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

/**
 * Created by leobert on 2021/9/18.
 */
sealed interface IUmlElementHandler {

    // TODO: 2021/9/18 修改返回，应该处理一个element的结构，而不是element之间的关系

    fun handle(
        from: UmlElement,
        relation: Relation,
        element: Element,
        diagram: ClassDiagram,
        graph: DAG<UmlElement>,
        cache: MutableSet<UmlElement>
    )

    object ClzHandler : IUmlElementHandler {
        override fun handle(
            from: UmlElement,
            relation: Relation,
            element: Element,
            diagram: ClassDiagram,
            graph: DAG<UmlElement>,
            cache: MutableSet<UmlElement>
        ) {
            if (shouldIgnoreEmlElement(element, diagram)) return

            //1. model.element to UmlElement
            val cur = UmlClass(diagram, element)
            graph.addEdge(Edge(from, cur, relation.type))

            if (cache.contains(cur)) return
            cache.add(cur)

            // TODO: 2021/9/19 parse dependency

            //2. parse extends
            // Generalization : only Object for enum
            element.takeIfInstance<TypeElement>()?.superclass?.let {
                it.ifElement()?.let { e ->
                    HandlerImpl.handle(
                        cur,
                        Relation.Generalization,
                        e,
                        diagram,
                        graph,
                        cache
                    )
                }
            }

            //3. parse implement
            val interfaces = element.takeIfInstance<TypeElement>()?.interfaces
            interfaces?.forEach {
                it.ifElement()?.let { e ->
                    HandlerImpl.handle(
                        cur,
                        Relation.Realization,
                        e,
                        diagram,
                        graph,
                        cache
                    )
                }

            }

            //...
        }
    }

    object EnumHandler : IUmlElementHandler {
        override fun handle(
            from: UmlElement,
            relation: Relation,
            element: Element,
            diagram: ClassDiagram,
            graph: DAG<UmlElement>,
            cache: MutableSet<UmlElement>
        ) {
            if (shouldIgnoreEmlElement(element, diagram)) return

            //1. model.element to UmlElement
            val cur = UmlEnum(diagram, element)
            graph.addEdge(Edge(from, cur, relation.type))

            if (cache.contains(cur)) return
            cache.add(cur)

            // TODO: 2021/9/19 parse dependency

            //2. parse extends
            // Generalization : only Object for enum

            //3. parse implement
            val interfaces = element.takeIfInstance<TypeElement>()?.interfaces
            interfaces?.forEach {
                it.ifElement()?.let { e ->
                    HandlerImpl.handle(
                        cur,
                        Relation.Realization,
                        e,
                        diagram,
                        graph,
                        cache
                    )
                }
            }

            //...

        }
    }

    object InterfaceHandler : IUmlElementHandler {
        override fun handle(
            from: UmlElement,
            relation: Relation,
            element: Element,
            diagram: ClassDiagram,
            graph: DAG<UmlElement>,
            cache: MutableSet<UmlElement>
        ) {
            if (shouldIgnoreEmlElement(element, diagram)) return

            //1. model.element to UmlElement
            val cur = UmlInterface(diagram, element)
            graph.addEdge(Edge(from, cur, relation.type))

            if (cache.contains(cur)) return
            cache.add(cur)

            // TODO: 2021/9/19 parse dependency

            //2. parse extends
            // Generalization : only Object for enum

            //3. parse implement
            val interfaces = element.takeIfInstance<TypeElement>()?.interfaces
            interfaces?.forEach {
                it.ifElement()?.let { e ->
                    HandlerImpl.handle(
                        cur,
                        Relation.Realization,
                        e,
                        diagram,
                        graph,
                        cache
                    )
                }
            }

            //...
        }
    }

    object HandlerImpl : IUmlElementHandler {
        private val strategy = mapOf(
            ElementKind.ENUM to EnumHandler,
            ElementKind.CLASS to ClzHandler,
            ElementKind.INTERFACE to InterfaceHandler
        )

        override fun handle(
            from: UmlElement,
            relation: Relation,
            element: Element,
            diagram: ClassDiagram,
            graph: DAG<UmlElement>,
            cache: MutableSet<UmlElement>
        ) {
            if (shouldIgnoreEmlElement(element, diagram)) return

            strategy[element.kind]?.handle(
                from, relation, element, diagram, graph, cache
            )
        }

    }
}