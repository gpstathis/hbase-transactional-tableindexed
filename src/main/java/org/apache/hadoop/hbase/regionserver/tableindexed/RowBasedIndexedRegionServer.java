package org.apache.hadoop.hbase.regionserver.tableindexed;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.tableindexed.RowBasedIndexSpecification;
import org.apache.hadoop.hbase.regionserver.HRegion;

/**
 * Specialized {@link IndexedRegionServer} instantiating
 * {@link RowBasedIndexedRegion}s so that secondary indexes defined through
 * {@link RowBasedIndexSpecification} can be used.
 * 
 * <p>
 * To enable secondary indexes defined using {@link RowBasedIndexSpecification},
 * modify hbase-site.xml to turn on the RowBasedIndexedRegionServer. This is
 * done by setting <i>hbase.regionserver.class</i> to
 * <i>org.apache.hadoop.hbase.ipc.IndexedRegionInterface</i> and
 * <i>hbase.regionserver.impl </i> to
 * <i>org.apache.hadoop.hbase.regionserver.tableindexed.RowBasedIndexedRegionServer</i>
 **/
public class RowBasedIndexedRegionServer extends IndexedRegionServer {
    
    public RowBasedIndexedRegionServer(final Configuration conf) throws IOException {
        super(conf);
    }
    
    @Override
    protected HRegion instantiateRegion(final HRegionInfo regionInfo) {
        HRegion r;
        try {
            r = new RowBasedIndexedRegion(HTableDescriptor.getTableDir(super.getRootDir(), regionInfo.getTableDesc().getName()),
                    super.hlog, super.getTransactionLog(), super.getFileSystem(), super.conf, regionInfo,
                    super.getFlushRequester(), super.getTransactionalLeases());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return r;
    }
}
