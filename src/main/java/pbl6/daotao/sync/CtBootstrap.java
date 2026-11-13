package pbl6.daotao.sync;
import java.sql.*;
/** Called explicitly by migration/setup, never from an HTTP request. */
public final class CtBootstrap {
 private CtBootstrap(){}
 public static void verifyKey(Connection c,CtResource r)throws SQLException {
  try(var s=c.prepareStatement("SELECT COUNT(*) FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id JOIN sys.columns col ON col.object_id=ic.object_id AND col.column_id=ic.column_id WHERE i.is_primary_key=1 AND i.object_id=OBJECT_ID(?) AND col.name=? AND col.system_type_id=TYPE_ID(?) AND ic.key_ordinal=1 AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal>0)=1")){
   s.setString(1,"dbo."+r.table());s.setString(2,r.primaryKey());s.setString(3,r.sqlKeyType());try(var rows=s.executeQuery()){if(!rows.next()||rows.getInt(1)!=1)throw new IllegalStateException("SIS key differs from implementation baseline");}
  }
 }
 public static void enableTableTracking(Connection c,CtResource r)throws SQLException {
  verifyKey(c,r);
  try(var s=c.prepareStatement("SELECT COUNT(*) FROM sys.change_tracking_tables WHERE object_id=OBJECT_ID(?)")){s.setString(1,"dbo."+r.table());try(var rows=s.executeQuery()){rows.next();if(rows.getInt(1)==1)return;}}
  try(var s=c.createStatement()){s.execute("ALTER TABLE dbo.["+r.table()+"] ENABLE CHANGE_TRACKING WITH(TRACK_COLUMNS_UPDATED=OFF)");}
 }
}
