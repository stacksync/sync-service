SELECT datname, pg_size_pretty(pg_database_size(datname)) FROM pg_database;


SELECT
   relname as "Table", 
   pg_size_pretty(pg_total_relation_size(relid)) As "Size", 
   pg_size_pretty(pg_total_relation_size(relid) - pg_relation_size(relid)) as "External Size"
   FROM pg_catalog.pg_statio_user_tables ORDER BY pg_total_relation_size(relid) DESC;
   
   
   
SELECT schemaname,relname,n_live_tup 
  FROM pg_stat_user_tables 
  ORDER BY n_live_tup DESC;