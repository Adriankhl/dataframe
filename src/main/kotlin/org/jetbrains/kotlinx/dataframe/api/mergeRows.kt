package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.Column
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.columns.values
import org.jetbrains.kotlinx.dataframe.impl.columns.asGroup
import org.jetbrains.kotlinx.dataframe.impl.columns.asTable
import org.jetbrains.kotlinx.dataframe.impl.columns.toColumns
import org.jetbrains.kotlinx.dataframe.toMany
import kotlin.reflect.KProperty

public fun <T> DataFrame<T>.mergeRows(vararg columns: String, dropNulls: Boolean = false): DataFrame<T> = mergeRows(dropNulls) { columns.toColumns() }
public fun <T> DataFrame<T>.mergeRows(vararg columns: Column, dropNulls: Boolean = false): DataFrame<T> = mergeRows(dropNulls) { columns.toColumns() }
public fun <T, C> DataFrame<T>.mergeRows(vararg columns: KProperty<C>, dropNulls: Boolean = false): DataFrame<T> = mergeRows(dropNulls) { columns.toColumns() }

public fun <T, C> DataFrame<T>.mergeRows(dropNulls: Boolean = false, columns: ColumnsSelector<T, C>): DataFrame<T> {
    return groupBy { except(columns) }.mapNotNullGroups {
        replace(columns).with {
            val column = it
            val filterNulls = dropNulls && column.hasNulls()
            val value = when (column.kind()) {
                org.jetbrains.kotlinx.dataframe.ColumnKind.Value -> column.toList().let { if (filterNulls) (it as List<Any?>).filterNotNull() else it }.toMany()
                org.jetbrains.kotlinx.dataframe.ColumnKind.Group -> column.asGroup().df
                org.jetbrains.kotlinx.dataframe.ColumnKind.Frame -> column.asTable().values.union()
            }
            var first = true
            column.map {
                if (first) {
                    first = false
                    value
                } else null
            }
        }[0..0]
    }.union()
}
