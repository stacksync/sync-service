------------------------------
-- Add item version
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS SETOF integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS integer AS $$
	INSERT INTO item_version( item_id, device_id, version, checksum, status, size, modified_at, committed_at) VALUES ( $2, $3, $4, $5, $6, $7, $8, now() );
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Insert chunk // this function is built dynamically
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION insert_chunk(uid uuid, sql text)
RETURNS SETOF integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION insert_chunk(uid uuid, sql text)
BEGIN
    RETURN QUERY EXECUTE sql;
END;
$$ LANGUAGE SQL;

------------------------------
-- Find chunks
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_chunks(uid uuid, item_version_id bigint)
RETURNS SETOF integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text);
	SELECT ivc.* FROM item_version_chunk ivc WHERE ivc.item_version_id=$2 ORDER BY ivc.chunk_order ASC;
$$ LANGUAGE plproxy;
