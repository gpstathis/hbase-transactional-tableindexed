package org.apache.hadoop.hbase.regionserver.tableindexed;

import java.io.IOException;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.Leases;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.tableindexed.IndexSpecification;
import org.apache.hadoop.hbase.client.tableindexed.IndexedTable;
import org.apache.hadoop.hbase.client.tableindexed.RowBasedIndexKeyGenerator;
import org.apache.hadoop.hbase.client.tableindexed.RowBasedIndexSpecification;
import org.apache.hadoop.hbase.regionserver.FlushRequester;
import org.apache.hadoop.hbase.regionserver.transactional.THLog;
import org.apache.hadoop.hbase.regionserver.wal.HLog;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Specialized {@link IndexedRegion} sub-class designed to work exclusively with
 * {@link RowBasedIndexSpecification} and {@link RowBasedIndexKeyGenerator} by
 * performing tailored secondary index table updates and deletes.
 */
public class RowBasedIndexedRegion extends IndexedRegion {

  private static final Log LOG = LogFactory.getLog(RowBasedIndexedRegion.class);
  
  public RowBasedIndexedRegion(final Path basedir, final HLog log, final THLog txLog, final FileSystem fs,
          final Configuration conf, final HRegionInfo regionInfo, final FlushRequester flushListener,
          final Leases trxLeases) throws IOException {
    super(basedir, log, txLog, fs, conf, regionInfo, flushListener, trxLeases);
  }
  
  @Override
  protected Delete makeDeleteToRemoveOldIndexEntry(IndexSpecification indexSpec, byte[] row,
      SortedMap<byte[], byte[]> oldColumnValues) throws IOException {
    for (byte[] indexedCol : indexSpec.getIndexedColumns()) {
      if (!oldColumnValues.containsKey(indexedCol)) {
        LOG.debug("Index [" + indexSpec.getIndexId()
            + "] not trying to remove old entry for row ["
            + Bytes.toString(row) + "] because col ["
            + Bytes.toString(indexedCol) + "] is missing");
        return null;
      }
    }

    byte[] oldIndexRow = indexSpec.getKeyGenerator().createIndexKey(row, oldColumnValues);
    LOG.debug("Index [" + indexSpec.getIndexId() + "] removing old entry [" + Bytes.toString(oldIndexRow) + "]");
    Delete del = new Delete(oldIndexRow);
    del.deleteColumns(IndexedTable.INDEX_COL_FAMILY, row);
    return del;
  }

  @Override
  protected Put makeIndexUpdate(IndexSpecification indexSpec, byte[] row,
      SortedMap<byte[], byte[]> columnValues) throws IOException {
    byte[] indexRow = indexSpec.getKeyGenerator().createIndexKey(row, columnValues);
    Put indexUpdate = new Put(indexRow);
    indexUpdate.add(IndexedTable.INDEX_COL_FAMILY, row, new byte[]{1});
    LOG.debug("Index [" + indexSpec.getIndexId() + "] adding new entry ["
        + Bytes.toString(indexUpdate.getRow()) + "] for row ["
        + Bytes.toString(row) + "]");

    return indexUpdate; 

  }
}
