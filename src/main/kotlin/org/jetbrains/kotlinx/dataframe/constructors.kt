package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.AddExpression
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.unbox
import org.jetbrains.kotlinx.dataframe.columns.ColumnAccessor
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnReference
import org.jetbrains.kotlinx.dataframe.columns.FrameColumn
import org.jetbrains.kotlinx.dataframe.impl.DataFrameImpl
import org.jetbrains.kotlinx.dataframe.impl.EmptyMany
import org.jetbrains.kotlinx.dataframe.impl.ManyImpl
import org.jetbrains.kotlinx.dataframe.impl.asList
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnAccessorImpl
import org.jetbrains.kotlinx.dataframe.impl.columns.createColumn
import org.jetbrains.kotlinx.dataframe.impl.columns.createComputedColumnReference
import org.jetbrains.kotlinx.dataframe.impl.columns.newColumn
import org.jetbrains.kotlinx.dataframe.impl.columns.newColumnWithActualType
import org.jetbrains.kotlinx.dataframe.impl.getType
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.full.withNullability

// region create ColumnAccessor

public fun <T> column(): ColumnDelegate<T> = ColumnDelegate()
public fun <T> column(name: String): ColumnAccessor<T> = ColumnAccessorImpl(name)
public fun <T> column(path: ColumnPath): ColumnAccessor<T> = ColumnAccessorImpl(path)
public fun <T> ColumnGroupReference.column(): ColumnDelegate<T> = ColumnDelegate<T>(this)
public fun <T> ColumnGroupReference.column(name: String): ColumnAccessor<T> = ColumnAccessorImpl(path() + name)
public fun <T> ColumnGroupReference.column(path: ColumnPath): ColumnAccessor<T> = ColumnAccessorImpl(this.path() + path)

public inline fun <reified T> column(name: String = "", noinline expression: RowExpression<Any?, T>): ColumnReference<T> = createComputedColumnReference(name, getType<T>(), expression)
public inline fun <T, reified C> DataFrame<T>.column(name: String = "", noinline expression: RowExpression<T, C>): ColumnReference<C> = createComputedColumnReference(name, getType<C>(), expression as RowExpression<Any?, C>)

public fun columnGroup(): ColumnDelegate<AnyRow> = column()
public fun columnGroup(name: String): ColumnAccessor<AnyRow> = column(name)
public fun columnGroup(path: ColumnPath): ColumnAccessor<AnyRow> = column(path)
public fun ColumnGroupReference.columnGroup(): ColumnDelegate<AnyRow> = ColumnDelegate(this)
public fun ColumnGroupReference.columnGroup(name: String): ColumnAccessor<AnyRow> = ColumnAccessorImpl(path() + name)
public fun ColumnGroupReference.columnGroup(path: ColumnPath): ColumnAccessor<AnyRow> = ColumnAccessorImpl(this.path() + path)

public fun frameColumn(): ColumnDelegate<AnyFrame> = column()
public fun frameColumn(name: String): ColumnAccessor<AnyFrame> = column(name)
public fun frameColumn(path: ColumnPath): ColumnAccessor<AnyFrame> = column(path)
public fun ColumnGroupReference.frameColumn(): ColumnDelegate<AnyFrame> = ColumnDelegate(this)
public fun ColumnGroupReference.frameColumn(name: String): ColumnAccessor<AnyFrame> = ColumnAccessorImpl(path() + name)
public fun ColumnGroupReference.frameColumn(path: ColumnPath): ColumnAccessor<AnyFrame> = ColumnAccessorImpl(this.path() + path)

public fun <T> columnMany(): ColumnDelegate<Many<T>> = column()
public fun <T> columnMany(name: String): ColumnAccessor<Many<T>> = column(name)

public class ColumnDelegate<T>(private val parent: ColumnGroupReference? = null) {
    public operator fun getValue(thisRef: Any?, property: KProperty<*>): ColumnAccessor<T> = named(property.name)

    public infix fun named(name: String): ColumnAccessor<T> =
        parent?.let { ColumnAccessorImpl(it.path() + name) } ?: ColumnAccessorImpl(name)
}

// endregion

// region create DataColumn

public inline fun <reified T> columnOf(vararg values: T): DataColumn<T> = createColumn(values.asIterable(), getType<T>(), true)

public fun columnOf(vararg values: AnyBaseColumn): DataColumn<AnyRow> = columnOf(values.asIterable())

public fun <T> columnOf(vararg frames: DataFrame<T>): FrameColumn<T> = columnOf(frames.asIterable())

public fun columnOf(columns: Iterable<AnyBaseColumn>): DataColumn<AnyRow> = DataColumn.createColumnGroup("", dataFrameOf(columns)) as DataColumn<AnyRow>

public fun <T> columnOf(frames: Iterable<DataFrame<T>>): FrameColumn<T> = DataColumn.createFrameColumn("", frames.toList())

public inline fun <reified T> column(values: Iterable<T>): DataColumn<T> = createColumn(values, getType<T>(), false)

// TODO: replace with extension
public inline fun <reified T> column(name: String, values: List<T>): DataColumn<T> = when {
    values.size > 0 && values.all { it is AnyCol } -> DataColumn.createColumnGroup(
        name,
        values.map { it as AnyCol }.toDataFrame()
    ) as DataColumn<T>
    else -> column(name, values, values.any { it == null })
}

// TODO: replace with extension
public inline fun <reified T> column(name: String, values: List<T>, hasNulls: Boolean): DataColumn<T> =
    DataColumn.createValueColumn(name, values, getType<T>().withNullability(hasNulls))

// endregion

// region create DataFrame

public fun dataFrameOf(columns: Iterable<AnyBaseColumn>): AnyFrame {
    val cols = columns.map { it.unbox() }
    if (cols.isEmpty()) return DataFrame.empty()
    return DataFrameImpl<Unit>(cols)
}

public fun dataFrameOf(vararg header: ColumnReference<*>): DataFrameBuilder = DataFrameBuilder(header.map { it.name() })

public fun dataFrameOf(vararg columns: AnyBaseColumn): AnyFrame = dataFrameOf(columns.asIterable())

public fun dataFrameOf(vararg header: String): DataFrameBuilder = dataFrameOf(header.toList())

public inline fun <T, reified C> dataFrameOf(first: T, second: T, vararg other: T, fill: (T) -> Iterable<C>): AnyFrame = dataFrameOf(listOf(first, second) + other, fill)

public fun <T> dataFrameOf(first: T, second: T, vararg other: T): DataFrameBuilder = dataFrameOf((listOf(first, second) + other).map { it.toString() })

public fun <T> dataFrameOf(header: Iterable<T>): DataFrameBuilder = dataFrameOf(header.map { it.toString() })

public inline fun <T, reified C> dataFrameOf(header: Iterable<T>, fill: (T) -> Iterable<C>): AnyFrame = header.map { value -> fill(value).asList().let { DataColumn.create(value.toString(), it) } }.toDataFrame()

public fun dataFrameOf(header: CharProgression): DataFrameBuilder = dataFrameOf(header.map { it.toString() })

public fun dataFrameOf(header: List<String>): DataFrameBuilder = DataFrameBuilder(header)

public class DataFrameBuilder(private val header: List<String>) {

    public operator fun invoke(vararg columns: AnyCol): AnyFrame = invoke(columns.asIterable())

    public operator fun invoke(columns: Iterable<AnyCol>): AnyFrame {
        val cols = columns.asList()
        require(cols.size == header.size) { "Number of columns differs from number of column names" }
        return cols.mapIndexed { i, col ->
            col.rename(header[i])
        }.toDataFrame()
    }

    public operator fun invoke(vararg values: Any?): AnyFrame = withValues(values.asIterable())

    @JvmName("invoke1")
    internal fun withValues(values: Iterable<Any?>): AnyFrame {
        val list = values.asList()

        val ncol = header.size

        require(header.size > 0 && list.size.rem(ncol) == 0) {
            "Number of values ${list.size} is not divisible by number of columns $ncol"
        }

        val nrow = list.size / ncol

        return (0 until ncol).map { col ->
            val colValues = (0 until nrow).map { row ->
                list[row * ncol + col]
            }
            DataColumn.createWithTypeInference(header[col], colValues)
        }.toDataFrame()
    }

    public operator fun invoke(args: Sequence<Any?>): AnyFrame = invoke(*args.toList().toTypedArray())

    public fun withColumns(columnBuilder: (String) -> AnyCol): AnyFrame = header.map(columnBuilder).toDataFrame()

    public inline operator fun <reified T> invoke(crossinline valuesBuilder: (String) -> Iterable<T>): AnyFrame = withColumns { name -> valuesBuilder(name).let { DataColumn.create(name, it.asList()) } }

    public inline fun <reified C> fill(nrow: Int, value: C): AnyFrame = withColumns { name -> DataColumn.createValueColumn(name, List(nrow) { value }, getType<C>().withNullability(value == null)) }

    public inline fun <reified C> nulls(nrow: Int): AnyFrame = fill<C?>(nrow, null)

    public inline fun <reified C> fillIndexed(nrow: Int, crossinline init: (Int, String) -> C): AnyFrame = withColumns { name -> DataColumn.create(name, List(nrow) { init(it, name) }) }

    public inline fun <reified C> fill(nrow: Int, crossinline init: (Int) -> C): AnyFrame = withColumns { name -> DataColumn.create(name, List(nrow, init)) }

    private inline fun <reified C> fillNotNull(nrow: Int, crossinline init: (Int) -> C) = withColumns { name -> DataColumn.createValueColumn(name, List(nrow, init), getType<C>()) }

    public fun randomInt(nrow: Int): AnyFrame = fillNotNull(nrow) { Random.nextInt() }

    public fun randomDouble(nrow: Int): AnyFrame = fillNotNull(nrow) { Random.nextDouble() }

    public fun randomFloat(nrow: Int): AnyFrame = fillNotNull(nrow) { Random.nextFloat() }

    public fun randomBoolean(nrow: Int): AnyFrame = fillNotNull(nrow) { Random.nextBoolean() }
}

public fun emptyDataFrame(nrow: Int): AnyFrame = DataFrame.empty(nrow)

// endregion

// region create ColumnPath

public fun pathOf(vararg columnNames: String): ColumnPath = ColumnPath(columnNames.asList())

// endregion

// region create DataColumn from DataFrame

public inline fun <T, reified R> ColumnsContainer<T>.newColumn(
    name: String = "",
    noinline expression: AddExpression<T, R>
): DataColumn<R> = newColumn(name, false, expression)

public inline fun <T, reified R> ColumnsContainer<T>.newColumn(
    name: String = "",
    useActualType: Boolean,
    noinline expression: AddExpression<T, R>
): DataColumn<R> {
    if (useActualType) return newColumnWithActualType(name, expression)
    return newColumn(getType<R>(), name, expression)
}

// endregion

// region create Many

public fun <T> emptyMany(): Many<T> = EmptyMany

public fun <T> manyOf(element: T): Many<T> = ManyImpl(listOf(element))

public fun <T> manyOf(vararg values: T): Many<T> = ManyImpl(listOf(*values))

// endregion