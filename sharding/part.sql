
------------------------------
-- Add device
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_device(uid uuid, name text, os text, last_ip inet, app_version text)
RETURNS uuid AS $$
DECLARE
	did uuid;
BEGIN
	did := (select uuid_generate_v1());	
	INSERT INTO device (id, name, user_id, os, created_at, last_access_at, last_ip, app_version) VALUES (did, $2, $1, $3, now(), now(), $4, $5);
	return did;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_device(uid uuid, name text, os text, last_ip inet, app_version text)
RETURNS uuid AS $$
DECLARE
	did uuid;
BEGIN
	did := (select uuid_generate_v1()); 
	INSERT INTO device (id, name, user_id, os, created_at, last_access_at, last_ip, app_version) VALUES (did, $2, $1, $3, now(), now(), $4, $5);
	return did;
END;
$$ LANGUAGE plpgsql;


------------------------------
-- Update device
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_device(uid uuid, did uuid, last_ip inet, app_version text)
RETURNS integer AS $$
	UPDATE device SET last_access_at = now(), last_ip = $3, app_version = $4 WHERE id = $2 and user_id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete device
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_device(uid uuid, did uuid)
RETURNS integer AS $$
	DELETE FROM device WHERE id = $2;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Add item version
------------------------------

-- Part

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS bigint AS $$
    INSERT INTO item_version( item_id, device_id, version, checksum, status, size, modified_at, committed_at) VALUES ( $2, $3, $4, $5, $6, $7, $8, now()) RETURNING id;
$$ LANGUAGE SQL;



------------------------------
-- Insert chunk
------------------------------

-- Part
CREATE OR REPLACE FUNCTION insert_chunk(uid uuid, item_version_id bigint, client_chunk_name bigint, chunk_order integer)
RETURNS integer AS $$
    INSERT INTO item_version_chunk( item_version_id, client_chunk_name, chunk_order ) VALUES ( $2, $3, $4 );
$$ LANGUAGE SQL;

------------------------------
-- Insert chunks // this function is built dynamically
------------------------------

-- Part

CREATE OR REPLACE FUNCTION dynamic_query(uid uuid, sql text)
RETURNS void AS $$
BEGIN
	EXECUTE sql;
END;
$$ LANGUAGE plpgsql;

-- Part

CREATE OR REPLACE FUNCTION dynamic_void_query(uid uuid, sql text)
RETURNS void AS $$
BEGIN
	EXECUTE sql;
END;
$$ LANGUAGE plpgsql;


------------------------------
-- Add item
------------------------------

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

-- Part

CREATE OR REPLACE FUNCTION update_item(uid uuid, wid uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint, item_id bigint)
RETURNS integer AS $$
    UPDATE item SET workspace_id = $2, latest_version = $3, parent_id = $4, filename = $5, mimetype = $6, is_folder = $7, client_parent_file_version = $8 WHERE id = $9;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Get items by workspace id
------------------------------

-- Part

CREATE OR REPLACE FUNCTION get_items_by_workspace_id(uid uuid, wid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[], chunks text[]) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.id AS version_id, iv.version, i.is_folder, i.workspace_id, iv.size, iv.status, i.mimetype, iv.checksum, iv.device_id, iv.modified_at, ARRAY[i.id] AS level_array FROM workspace w INNER JOIN item i ON w.id = i.workspace_id INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE w.id = $2 AND i.parent_id IS NULL UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.device_id, iv2.modified_at, q.level_array || i2.id FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version WHERE i2.workspace_id=$2 ) SELECT array_upper(level_array, 1) as level, q.*, get_chunks(q.version_id) AS chunks FROM q ORDER BY level_array ASC;
$$ LANGUAGE SQL;

------------------------------
-- Get items by id
------------------------------


-- Part

CREATE OR REPLACE FUNCTION get_items_by_id(uid uuid, iid bigint)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[]) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.id AS version_id, iv.version, i.is_folder, i.workspace_id, iv.size, iv.status, i.mimetype, iv.checksum, iv.device_id, iv.modified_at, ARRAY[i.id] AS level_array FROM item i INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE i.id = $2 UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.device_id, iv2.modified_at, q.level_array || i2.id FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version) SELECT array_upper(level_array, 1) as level, q.* FROM q ORDER BY level_array ASC;
$$ LANGUAGE SQL;


------------------------------
-- Find item by user id
------------------------------

-- Part

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, device_id uuid, workspace_id uuid, version integer, is_folder boolean, size bigint, status text, mimetype text, checksum bigint, modified_at timestamp, level_array bigint[], path text ) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.device_id, i.workspace_id, iv.version, i.is_folder, iv.size, iv.status, i.mimetype, iv.checksum, iv.modified_at, ARRAY[i.id] AS level_array, '/' AS path FROM user1 u INNER JOIN workspace_user wu ON u.id = wu.user_id INNER JOIN item i ON wu.workspace_id = i.workspace_id INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE u.id = $1 AND i.parent_id IS NULL UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.device_id, i2.workspace_id, iv2.version, i2.is_folder, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.modified_at, q.level_array || i2.id, q.path || q.filename::TEXT || '/' FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version WHERE array_upper(level_array, 1) < 1) SELECT array_upper(level_array, 1) as level, q.* FROM q ORDER BY level_array ASC;

$$ LANGUAGE SQL;


------------------------------
-- Migrate item
------------------------------

-- Part

CREATE OR REPLACE FUNCTION migrate_item(uid uuid, iid bigint, wid uuid)
RETURNS INTEGER AS $$
    WITH RECURSIVE q AS ( SELECT i.* FROM item i WHERE i.id = $2 UNION ALL SELECT i2.* FROM q JOIN item i2 ON i2.parent_id = q.id ) UPDATE item i3 SET workspace_id = $3 FROM q WHERE q.id = i3.id;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Insert user
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
	INSERT INTO user1 (id, email, name, swift_user, swift_account, quota_limit, quota_used) VALUES ($1, $2, $3, $4, $5, $6, $7);
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Update
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
	UPDATE user1 SET email = $2, name = $3, swift_user = $4, swift_account = $5, quota_limit = $6, quota_used = $7 WHERE id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_user(uid uuid)
RETURNS integer AS $$
	DELETE FROM user1 WHERE id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Find user by itemid
------------------------------

-- Part

CREATE OR REPLACE FUNCTION find_user_by_item_id(id bigint)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
	SELECT u.* FROM item i INNER JOIN workspace_user wu ON i.workspace_id = wu.workspace_id INNER JOIN user1 u ON wu.user_id = u.id WHERE i.id = $1;
$$ LANGUAGE SQL;

------------------------------
-- Add workspace
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_workspace(uid uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text)
RETURNS uuid AS $$
DECLARE
    wid uuid;
BEGIN
    wid := (select uuid_generate_v1());   	 
    INSERT INTO workspace (id, latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url) VALUES (wid, $2, $3, $4, $5, $6, $7);
    INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES (wid, uid, 'default', NULL);
    return wid;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Update workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
    UPDATE workspace_user SET workspace_name = $3, parent_item_id = $4, modified_at = now() WHERE user_id = $1 AND workspace_id = $2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
    DELETE FROM workspace WHERE id = $2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Add user into a workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION add_user_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
    INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES ($2, $1, $3, $4);
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete user into a workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_user_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
    DELETE FROM workspace_user WHERE user_id=$1 AND workspace_id=$2;
    SELECT 1;
$$ LANGUAGE SQL;


-- Delete user1
CREATE OR REPLACE FUNCTION delete_user1()
RETURNS integer AS $$
    DELETE FROM user1;
    SELECT 1;
$$ LANGUAGE SQL;

