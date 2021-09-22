package osp.leobert.android.reporter.diagram.core

import osp.leobert.android.maat.dag.DAG
import osp.leobert.android.reporter.diagram.Utils.ifElement
import osp.leobert.android.reporter.diagram.Utils.ifTypeElement
import osp.leobert.android.reporter.diagram.Utils.refersIfDeclaredType
import osp.leobert.android.reporter.diagram.notation.ClassDiagram
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter

/**
 * <p><b>Package:</b> osp.leobert.android.reporter.diagram.core </p>
 * <p><b>Project:</b> ReportPrinter </p>
 * <p><b>Classname:</b> UmlElement </p>
 * <p><b>Description:</b> TODO </p>
 * Created by leobert on 2021/9/18.
 */
abstract class UmlElement(val diagram: ClassDiagram?, val element: Element?) {
    val name: String = element?.simpleName?.toString() ?: "Stub"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UmlElement

        if (diagram != other.diagram) return false
        if (element != other.element) return false

        return true
    }

    override fun hashCode(): Int {
        var result = diagram.hashCode()
        result = 31 * result + (element?.hashCode() ?: 0)
        return result
    }

    abstract fun umlElement(context: MutableSet<UmlElement>): String
    abstract fun parseFieldAndMethod(diagram: ClassDiagram, graph: DAG<UmlElement>, cache: MutableSet<UmlElement>)
    abstract fun drawField(fieldDrawer: FieldDrawer, builder: StringBuilder, context: MutableSet<UmlElement>)

    protected fun handleDependencyViaField(variableElement: VariableElement, diagram: ClassDiagram, graph: DAG<UmlElement>, cache: MutableSet<UmlElement>) {
        val vType = variableElement.asType()
        if (vType.kind.isPrimitive) {
            return
        }

        val e = variableElement.asType().ifElement()
        //todo consider Collections(wildcard) and arrays!


        //1. handle wildcard
        val refers = variableElement.asType().refersIfDeclaredType()
        if (!refers.isNullOrEmpty()) {
            refers.forEach { refer ->
                refer.ifElement()?.let { ele ->
                    IUmlElementHandler.HandlerImpl.handle(from = this, relation = Relation.Dependency,
                            element = ele, diagram = diagram, graph = graph, cache = cache)

                }
            }
            return
        }

        //handle normal
        if (e != null) {
            IUmlElementHandler.HandlerImpl.handle(from = this, relation = Relation.Dependency, element = e, diagram = diagram, graph = graph, cache = cache)
            return
        }
    }
}

//inline operator fun UmlElement.getValue(thisObj: Any?, property: KProperty<*>): String = this.element


class UmlStub private constructor() : UmlElement(null, null) {

    companion object {
        val sInstance = UmlStub()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun umlElement(context: MutableSet<UmlElement>): String {
        return ""
    }

    override fun parseFieldAndMethod(diagram: ClassDiagram, graph: DAG<UmlElement>, cache: MutableSet<UmlElement>) {
    }

    override fun drawField(fieldDrawer: FieldDrawer, builder: StringBuilder, context: MutableSet<UmlElement>) {
    }

}

class UmlClass(diagram: ClassDiagram, element: Element) : UmlElement(diagram, element) {

    companion object {
        private val drawer = ElementDrawer(NameDrawer.ClzNameDrawer).apply {
            this.modifierDrawer.remove(AbstractTypeDrawer)
            this.modifierDrawer.add(AbstractTypeDrawer)
        }
    }

    private val mFields: MutableSet<VariableElement> = LinkedHashSet()
    private val mMethods: MutableSet<ExecutableElement> = LinkedHashSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun umlElement(context: MutableSet<UmlElement>): String {
        val builder = StringBuilder()
        drawer.drawAspect(builder, this, context)
        return builder.toString()
    }

    override fun parseFieldAndMethod(diagram: ClassDiagram, graph: DAG<UmlElement>, cache: MutableSet<UmlElement>) {
        val tElement = element.ifTypeElement() ?: return

        val fields = ElementFilter.fieldsIn(tElement.enclosedElements)
        mFields.clear()
        mFields.addAll(fields)

        fields.forEach {
            handleDependencyViaField(it, diagram, graph, cache)
        }

        val methods = ElementFilter.methodsIn(tElement.enclosedElements)
        mMethods.clear()
        mMethods.addAll(methods)


    }

    override fun drawField(fieldDrawer: FieldDrawer, builder: StringBuilder, context: MutableSet<UmlElement>) {
        mFields.let {
            builder.append(".. fields ..").append(RETURN)
            it.forEach { field ->
                fieldDrawer.invokeDraw(builder, field, context)
                //todo 使用FieldDrawer this is a test
                builder.append("'").append(field.toString())
                        .append(":").append(field.asType().toString())
                        .append("//").append(field.asType().ifElement()?.simpleName)
                        .append(RETURN)
            }
        }
    }

}

class UmlEnum(diagram: ClassDiagram, element: Element) : UmlElement(diagram, element) {
    companion object {
        private val drawer = ElementDrawer(NameDrawer.EnumNameDrawer)
    }

    private val mFields: MutableSet<VariableElement> = LinkedHashSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun umlElement(context: MutableSet<UmlElement>): String {
        val builder = StringBuilder()
        drawer.drawAspect(builder, this, context)
        return builder.toString()
    }

    override fun parseFieldAndMethod(diagram: ClassDiagram, graph: DAG<UmlElement>, cache: MutableSet<UmlElement>) {
        val tElement = element.ifTypeElement() ?: return

        val fields = ElementFilter.fieldsIn(tElement.enclosedElements)
        mFields.clear()
        mFields.addAll(fields)

        fields.forEach {
            if (it.asType() != element?.asType())
                handleDependencyViaField(it, diagram, graph, cache)
        }

    }

    override fun drawField(fieldDrawer: FieldDrawer, builder: StringBuilder, context: MutableSet<UmlElement>) {
        mFields.filter {
            it.asType() == element?.asType()
        }.let {
            builder.append(".. enums ..").append(RETURN)
            it.forEach { field ->
                builder.append(field.toString()).append(RETURN)
            }
        }

        mFields.filter {
            it.asType() != element?.asType()
        }.let {
            builder.append(".. fields ..").append(RETURN)
            it.forEach { field ->
                fieldDrawer.invokeDraw(builder, field, context)
                //todo 使用FieldDrawer this is a test
                builder.append("'").append(field.toString())
                        .append(":").append(field.asType().toString())
                        .append("//").append(field.modifiers)
                        .append("//").append(field.asType().ifElement()?.simpleName)
                        .append(RETURN)
            }
        }
    }

}

class UmlInterface(diagram: ClassDiagram, element: Element) : UmlElement(diagram, element) {
    companion object {
        private val drawer = ElementDrawer(NameDrawer.InterfaceNameDrawer)
    }

    private val mFields: MutableSet<VariableElement> = LinkedHashSet()
    private val mMethods: MutableSet<ExecutableElement> = LinkedHashSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun umlElement(context: MutableSet<UmlElement>): String {
        val builder = StringBuilder()
        drawer.drawAspect(builder, this, context)
        return builder.toString()
    }

    override fun parseFieldAndMethod(diagram: ClassDiagram, graph: DAG<UmlElement>, cache: MutableSet<UmlElement>) {
        val tElement = element.ifTypeElement() ?: return

        val fields = ElementFilter.fieldsIn(tElement.enclosedElements)
        mFields.clear()
        mFields.addAll(fields)

        fields.forEach {
            handleDependencyViaField(it, diagram, graph, cache)
        }

        val methods = ElementFilter.methodsIn(tElement.enclosedElements)
        mMethods.clear()
        mMethods.addAll(methods)


    }

    override fun drawField(fieldDrawer: FieldDrawer, builder: StringBuilder, context: MutableSet<UmlElement>) {
        //todo test
        mFields.forEach { field: VariableElement ->
            builder.append(field.toString()).append(RETURN)
        }
    }

}

