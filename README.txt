This project adds row-based indexing to the transactional and indexing extension for hbase. Row-based indexing allows applications to reliably paginate through index records using org.apache.hadoop.hbase.filter.ColumnPaginationFilter (currently, org.apache.hadoop.hbase.filter.PageFilter cannot guarantee
that the number of results returned to a client are <= page size; this is because the filter is applied separately on different region servers). 

Installation:
 Drop the jar in the classpath of your application
 
Configuration: 
 One of the two regionserver extensions must be turned on by setting the appropriate configuration
 (in hbase-site.xml). 
 
 To enable the indexing extension: set 
 'hbase.regionserver.class' to 'org.apache.hadoop.hbase.ipc.IndexedRegionInterface' 
 and 
 'hbase.regionserver.impl' to 'org.apache.hadoop.hbase.regionserver.tableindexed.IndexedRegionServer'
 
 To enable the row-based indexing extension: set 
 'hbase.regionserver.class' to 'org.apache.hadoop.hbase.ipc.IndexedRegionInterface' 
 and 
 'hbase.regionserver.impl' to 'org.apache.hadoop.hbase.regionserver.tableindexed.RowBasedIndexedRegionServer'
 
 To enable the transactional extension (which includes the indexing): set 
 'hbase.regionserver.class' to 'org.apache.hadoop.hbase.ipc.TransactionalRegionInterface' 
  and
 'hbase.regionserver.impl' to 'org.apache.hadoop.hbase.regionserver.transactional.TransactionalRegionServer'
and
 'hbase.hlog.splitter.impl' to 'org.apache.hadoop.hbase.regionserver.transactional.THLogSplitter'
 
 Currently you have to manually create the GLOBAL_TRX_LOG table with HBaseBackedTransactionLogger.createTable() before you start using any transactions.
 
 For more details, looks at the package.html in the appropriate client package of the source. 
