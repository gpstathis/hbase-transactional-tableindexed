package org.apache.hadoop.hbase.client.tableindexed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.util.Bytes;

public class RowBasedIndexedTable extends IndexedTable
{

  public RowBasedIndexedTable(final Configuration conf, final byte[] tableName) throws IOException
  {
    super(conf, tableName);
  }

  /**
   * Open up an indexed scanner on a {@link RowBasedIndexSpecification} type
   * index. Results will come back in the indexed order, but will contain
   * RowResults from the original table. The indexRow parameter is always
   * required and corresponds to the value of the indexed based table column
   * against which we wish to filter. This value is used to limit the secondary
   * index table scan to the specific row containing the indexed values.
   * 
   * @param indexId
   * @param indexRow
   *          Required: should be the value in the base table column against
   *          which we want to filter.
   * @param indexColumn
   *          Required so that we verify that we are querying against the
   *          correct index.
   * @param baseColumns
   * @return
   * @throws IOException
   * @throws IndexNotFoundException
   * @throws IllegalArgumentException
   */
  public ResultScanner getIndexedScanner(String indexId, final byte[] indexRow, 
      byte[] indexColumn, final byte[][] baseColumns) throws IOException, IndexNotFoundException, IllegalArgumentException {
    return this.getIndexedScanner(indexId, indexRow, indexColumn, baseColumns, null);
  }
  
  
  /**
   * Open up an indexed scanner on a {@link RowBasedIndexSpecification} type
   * index. Results will come back in the indexed order, but will contain
   * RowResults from the original table. The indexRow parameter is always
   * required and corresponds to the value of the indexed based table column
   * against which we wish to filter. This value is used to limit the secondary
   * index table scan to the specific row containing the indexed values.
   * 
   * @param indexId
   * @param indexRow
   *          Required: should be the value in the base table column against
   *          which we want to filter.
   * @param indexColumn
   *          Required so that we verify that we are querying against the
   *          correct index.
   * @param baseColumns
   * @return
   * @throws IOException
   * @throws IndexNotFoundException
   * @throws IllegalArgumentException
   */
  public ResultScanner getIndexedScanner(String indexId, final byte[] indexRow, 
      byte[] indexColumn, final byte[][] baseColumns, FilterList filters) throws IOException, IndexNotFoundException, IllegalArgumentException {
    if(null==indexRow) {
      throw new IllegalArgumentException("the index row parameter cannot be null");
    }
    if(null==indexColumn) {
      throw new IllegalArgumentException("the index column parameter cannot be null");
    }
    IndexSpecification indexSpec = this.indexedTableDescriptor.getIndex(indexId);
    if (indexSpec == null) {
      throw new IndexNotFoundException("Index " + indexId
          + " not defined in table "
          + super.getTableDescriptor().getNameAsString());
    }
    verifyIndexColumns((null!=indexColumn)?new byte[][]{indexColumn}:null, indexSpec);
    // TODO, verify/remove index columns from baseColumns

    HTable indexTable = indexIdToTable.get(indexId);

    // Start and stop on the same row.
    Scan indexScan = new Scan();
    if (null != filters) {
        indexScan.setFilter(filters);
    }
    
    indexScan.setStartRow(indexRow);
    indexScan.setStopRow(Bytes.toBytes(Bytes.toString(indexRow).concat("z")));
    ResultScanner indexScanner = indexTable.getScanner(indexScan);

    return new RowBasedScannerWrapper(indexScanner, baseColumns, indexRow);
  }
  
  private class RowBasedScannerWrapper implements ResultScanner {

    private ResultScanner indexScanner;
    private byte[][] columns;
    private Result indexResult;
    private Iterator<byte[]> qualifiers;
    private byte[] indexRow;

    public RowBasedScannerWrapper(ResultScanner indexScanner, byte[][] columns, final byte[] indexRow) {
      this.indexScanner = indexScanner;
      this.columns = columns;
      this.indexRow = indexRow;
    }

    /** {@inheritDoc} */
    public Result next() throws IOException {
      Result[] result = next(1);
      if (result == null || result.length < 1)
        return null;
      return result[0];
    }

    /** {@inheritDoc} */
    public Result[] next(int nbRows) throws IOException {
      if(this.indexResult==null) {
        indexResult = indexScanner.next();
        if (indexResult == null) {
          LOG.debug("Missing index row for indexed key: ["+Bytes.toString(this.indexRow)+"]");
          return null;
        }
        qualifiers = indexResult.getFamilyMap(INDEX_COL_FAMILY).navigableKeySet().iterator();
      }
      List<Result> resultsArray = new ArrayList<Result>();
      int resultCount = nbRows;
      while(qualifiers.hasNext() && resultCount>0) {
        byte[] baseRow = qualifiers.next();
        LOG.debug("next index column [" + Bytes.toString(baseRow) + "]");
        Result baseResult = null;
        if (columns == null || columns.length == 0) {
          Set<byte[]> baseColumnsByte = new HashSet<byte[]>();
          if(null!=columns)
          {
              for (byte[] baseColumn : columns)
                  baseColumnsByte.add(baseColumn);
          }
          else
              baseColumnsByte = RowBasedIndexedTable.this.getTableDescriptor().getFamiliesKeys();
          columns=baseColumnsByte.toArray(new byte[0][0]);
        }
        LOG.debug("Going to base table for remaining columns");
        Get baseGet = new Get(baseRow);
        baseGet.addColumns(columns);
        baseResult = RowBasedIndexedTable.this.get(baseGet);
        
        List<KeyValue> results = new ArrayList<KeyValue>();
        if (baseResult != null) {
          List<KeyValue> list = baseResult.list();
          if (list != null) {
            results.addAll(list);
          }
        }
        
        resultsArray.add(new Result(results));
        resultCount--;
      }
      
      return resultsArray.toArray(new Result[]{});
    }

    /** {@inheritDoc} */
    public void close() {
      indexScanner.close();
    }
    
    // Copied from HTable.ClientScanner.iterator()
    public Iterator<Result> iterator() {
      return new Iterator<Result>() {
        // The next RowResult, possibly pre-read
        Result next = null;
        
        // return true if there is another item pending, false if there isn't.
        // this method is where the actual advancing takes place, but you need
        // to call next() to consume it. hasNext() will only advance if there
        // isn't a pending next().
        public boolean hasNext() {
          if (next == null) {
            try {
              next = RowBasedScannerWrapper.this.next();
              return next != null;
            } catch (IOException e) {
              throw new RuntimeException(e);
            }            
          }
          return true;
        }

        // get the pending next item and advance the iterator. returns null if
        // there is no next item.
        public Result next() {
          // since hasNext() does the real advancing, we call this to determine
          // if there is a next before proceeding.
          if (!hasNext()) {
            return null;
          }
          
          // if we get to here, then hasNext() has given us an item to return.
          // we want to return the item and then null out the next pointer, so
          // we use a temporary variable.
          Result temp = next;
          next = null;
          return temp;
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

  }
}
