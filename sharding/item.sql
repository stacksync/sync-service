------------------------------
-- Get by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_item_by_id(uid uuid, iid bigint)
RETURNS TABLE(id bigint, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT * FROM item WHERE id = iid;
$$ LANGUAGE plproxy;

------------------------------
-- Add item
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_item(uid uuid, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint)
RETURNS bigint AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part
CREATE OR REPLACE FUNCTION add_item(uid uuid, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint)
RETURNS bigint AS $$
DECLARE
    iid bigint;
BEGIN
    INSERT INTO item (workspace_id, latest_version, parent_id, filename, mimetype, is_folder, client_parent_file_version ) VALUES ( $2, $3, $4, $5, $6, $7, $8) RETURNING id INTO iid;
    RETURN iid;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Update item
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_item(uid uuid, wid uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint, item_id bigint)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION update_item(uid uuid, wid uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint, item_id bigint)
RETURNS integer AS $$
    UPDATE item SET workspace_id = $2, latest_version = $3, parent_id = $4, filename = $5, mimetype = $6, is_folder = $7, client_parent_file_version = $8 WHERE id = $9;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Get items by workspace id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_items_by_workspace_id(uid uuid, wid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[], chunks text[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION get_items_by_workspace_id(uid uuid, wid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[], chunks text[]) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.id AS version_id, iv.version, i.is_folder, i.workspace_id, iv.size, iv.status, i.mimetype, iv.checksum, iv.device_id, iv.modified_at, ARRAY[i.id] AS level_array FROM workspace w INNER JOIN item i ON w.id = i.workspace_id INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE w.id = $2 AND i.parent_id IS NULL UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.device_id, iv2.modified_at, q.level_array || i2.id FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version WHERE i2.workspace_id=$2 ) SELECT array_upper(level_array, 1) as level, q.*, get_chunks(q.version_id) AS chunks FROM q ORDER BY level_array ASC;
$$ LANGUAGE SQL;

------------------------------
-- Get items by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_items_by_id(uid uuid, iid bigint)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION get_items_by_id(uid uuid, iid bigint)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[]) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.id AS version_id, iv.version, i.is_folder, i.workspace_id, iv.size, iv.status, i.mimetype, iv.checksum, iv.device_id, iv.modified_at, ARRAY[i.id] AS level_array FROM item i INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE i.id = $2 UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.device_id, iv2.modified_at, q.level_array || i2.id FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version) SELECT array_upper(level_array, 1) as level, q.* FROM q ORDER BY level_array ASC;
$$ LANGUAGE SQL;

------------------------------
-- Find item by id -> NO ES FA SERVIR ENLLOC
------------------------------

CREATE OR REPLACE FUNCTION find_item_by_id(uid uuid, iid bigint, max_level integer)
RETURNS  TABLE(id bigint, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(id::text);
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION find_item_by_id(uid uuid, iid bigint, max_level integer)
RETURNS TABLE(id bigint, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint) AS $$
BEGIN
--TODO CREAR LA FUNCIÓ AQUESTA
$$ LANGUAGE plpgsql;

------------------------------
-- Find item by user id
------------------------------

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, device_id uuid, workspace_id uuid, version integer, is_folder boolean, size bigint, status text, mimetype text, checksum bigint, modified_at timestamp, level_array bigint[], path text ) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, device_id uuid, workspace_id uuid, version integer, is_folder boolean, size bigint, status text, mimetype text, checksum bigint, modified_at timestamp, level_array bigint[], path text ) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.device_id, i.workspace_id, iv.version, i.is_folder, iv.size, iv.status, i.mimetype, iv.checksum, iv.modified_at, ARRAY[i.id] AS level_array, '/' AS path FROM user1 u INNER JOIN workspace_user wu ON u.id = wu.user_id INNER JOIN item i ON wu.workspace_id = i.workspace_id INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE u.id = $1 AND i.parent_id IS NULL UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.device_id, i2.workspace_id, iv2.version, i2.is_folder, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.modified_at, q.level_array || i2.id, q.path || q.filename::TEXT || '/' FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version WHERE array_upper(level_array, 1) < 1) SELECT array_upper(level_array, 1) as level, q.* FROM q ORDER BY level_array ASC;

$$ LANGUAGE SQL;

------------------------------
-- Find item versions by item id
------------------------------
item_id | parent_id | client_parent_file_version | filename | is_folder | mimetype | workspace_id | version | size | status | checksum | device_id | modified_at | level | path

--Proxy

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid, file_id bigint)
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text,  is_folder boolean, mimetype text, workspace_id uuid, version integer, size bigint, status text,  checksum bigint, device_id uuid, modified_at timestamp, level integer, path text ) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, iv.version, iv.size, iv.status, iv.checksum, iv.device_id, iv.modified_at, '1' AS level, '' AS path FROM item i inner join item_version iv on iv.item_id = i.id where i.id = $2 ORDER BY iv.version DESC;
$$ LANGUAGE plproxy;

------------------------------
-- Migrate item
------------------------------

--Proxy

CREATE OR REPLACE FUNCTION migrate_item(uid uuid, iid bigint, wid uuid)
RETURNS INTEGER AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION migrate_item(uid uuid, iid bigint, wid uuid)
RETURNS INTEGER AS $$
    WITH RECURSIVE q AS ( SELECT i.* FROM item i WHERE i.id = $2 UNION ALL SELECT i2.* FROM q JOIN item i2 ON i2.parent_id = q.id ) UPDATE item i3 SET workspace_id = $3 FROM q WHERE q.id = i3.id;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Get chunks to migrate
------------------------------

--Proxy

CREATE OR REPLACE FUNCTION get_chunks_to_migrate(uid uuid, iid bigint)
RETURNS TABLE(chunks text[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT get_unique_chunks_to_migrate($2) AS chunks;
$$ LANGUAGE plproxy;

