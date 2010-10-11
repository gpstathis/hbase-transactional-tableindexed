package org.apache.hadoop.hbase.client.tableindexed;


/**
 * Specialized {@link IndexSpecification} creating row-based secondary index
 * tables. Base table rows with the same indexed column value have their row
 * keys stored as column qualifiers on the same secondary index table row. The
 * key for that row is the indexed column value from the base table. This allows
 * to avoid expensive secondary index table scans and provides faster access for
 * applications such as foreign key indexing or queries such as
 * "find all table A rows whose familyA:columnB value is X".
 */
public class RowBasedIndexSpecification extends IndexSpecification {

  /**
   * Construct a row-based index spec for a single column using a
   * {@link RowBasedIndexKeyGenerator} under the covers.
   * 
   * @param indexId
   * @param indexedColumn
   */
  public RowBasedIndexSpecification(String indexId, byte[] indexedColumn) {
    super(indexId, new byte[][] { indexedColumn }, null, 
      new RowBasedIndexKeyGenerator(indexedColumn));
  }

}
