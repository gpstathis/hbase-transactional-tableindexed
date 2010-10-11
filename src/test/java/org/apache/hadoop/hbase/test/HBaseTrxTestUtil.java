/*
 * $Id$
 * Created on Aug 6, 2010
 * 
 */
package org.apache.hadoop.hbase.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.ipc.IndexedRegionInterface;
import org.apache.hadoop.hbase.regionserver.tableindexed.IndexedRegionServer;
import org.apache.hadoop.hbase.regionserver.tableindexed.RowBasedIndexedRegionServer;
import org.apache.hadoop.hbase.regionserver.transactional.THLogSplitter;


public class HBaseTrxTestUtil {

    public static void configureForIndexingAndTransactions(Configuration config) {
        config.set(HConstants.REGION_SERVER_CLASS, IndexedRegionInterface.class.getName());
        config.set(HConstants.REGION_SERVER_IMPL, IndexedRegionServer.class.getName());
        config.set("hbase.hlog.splitter.impl", THLogSplitter.class.getName());

        config.setInt("ipc.client.connect.max.retries", 5); // reduce ipc retries
        config.setInt("ipc.client.timeout", 20000); // and ipc timeout
        config.setInt("hbase.client.pause", 20000); // increase client timeout
        config.setInt("hbase.client.retries.number", 10); // increase HBase retries
    }

    public static void configureForRowBasedIndexing(Configuration config) {
        config.set(HConstants.REGION_SERVER_CLASS, IndexedRegionInterface.class.getName());
        config.set(HConstants.REGION_SERVER_IMPL, RowBasedIndexedRegionServer.class.getName());
        config.set("hbase.hlog.splitter.impl", THLogSplitter.class.getName());

        config.setInt("ipc.client.connect.max.retries", 5); // reduce ipc retries
        config.setInt("ipc.client.timeout", 20000); // and ipc timeout
        config.setInt("hbase.client.pause", 20000); // increase client timeout
        config.setInt("hbase.client.retries.number", 10); // increase HBase retries
    }
}
