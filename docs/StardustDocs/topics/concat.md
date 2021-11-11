[//]: # (title: concat)

<!---IMPORT org.jetbrains.kotlinx.dataframe.samples.api.Modify-->

Returns `DataFrame` with the union of rows from several given DataFrames.

To union columns instead of rows, see [add](add.md)

<!---FUN concat-->

```kotlin
df.concat(otherDf)
```

<!---END-->

You can also concatenate a list of DataFrames. `.concat()` is available for:
* `Iterable<DataFrame>`
* `Iterable<DataRow>`
* `GroupedDataFrame`
* [`FrameColumn`](DataColumn.md#FrameColumn):

<!---FUN concatIterable-->

```kotlin
listOf(df[0..1], df[4..5]).concat()
```

<!---END-->

<!---FUN concatFrameColumn-->

```kotlin
val frameColumn by columnOf(df[0..1], df[4..5])
frameColumn.concat()
```

<!---END-->

<!---FUN concatGroupedDataFrame-->

```kotlin
df.groupBy { name }.concat()
```

<!---END-->

## Schema unification

If input DataFrames have different schemas, every column in resulting `DataFrame` will have the most common type of the original columns with the same name. 

For example, if one `DataFrame` has column `A` of type `Int` and other `DataFrame` has column `A` of type `Double`, resulting `DataFrame` will have column `A` of type `Number`.

Missing columns in DataFrames will be filled with `null`.