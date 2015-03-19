------------------------------
-- Add item version
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS bigint AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS bigint AS $$
BEGIN
	INSERT INTO item_version( item_id, device_id, version, checksum, status, size, modified_at, committed_at) VALUES ( $2, $3, $4, $5, $6, $7, $8, now());
	return NEW;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Find by Item and Version
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_by_itemid_and_version(uid uuid, item_id bigint, version bigint)
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, is_folder boolean, mimetype text, workspace_id uuid, version integer, device_id uuid, checksum bigint, status text, size bigint, modified_at timestamp, chunks text[]) AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text);
	SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, iv.version, iv.device_id, iv.checksum, iv.status, iv.size, iv.modified_at, get_chunks(iv.id) AS chunks FROM item_version iv INNER JOIN item i ON i.id = iv.item_id WHERE iv.item_id = $2 and iv.version = $3;

$$ LANGUAGE plproxy;

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
